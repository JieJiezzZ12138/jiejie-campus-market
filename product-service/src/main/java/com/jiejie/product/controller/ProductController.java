package com.jiejie.product.controller;

import com.jiejie.common.Result;
import com.jiejie.common.utils.JwtUtils;
import com.jiejie.product.entity.Product;
import com.jiejie.product.entity.ProductReview;
import com.jiejie.product.mapper.ProductFavoriteMapper;
import com.jiejie.product.mapper.ProductMapper;
import com.jiejie.product.mapper.ProductCategoryMapper;
import com.jiejie.product.mapper.ProductReviewMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/product")
public class ProductController {
    private static final int LEGACY_IMAGE_URL_SAFE_LEN = 480;

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductFavoriteMapper productFavoriteMapper;
    @Autowired
    private ProductReviewMapper productReviewMapper;
    @Autowired
    private ProductCategoryMapper productCategoryMapper;
    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     * 1. 查询商品列表
     */
    @GetMapping("/list")
    @Cacheable(cacheNames = "product:list",
            key = "T(java.util.Objects).toString(#p0, '') + ':' + T(java.util.Objects).toString(#p3, '')",
            condition = "#p1 == null || #p1.isBlank()")
    public Result list(@RequestParam(value = "category", required = false) String category,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "searchMode", defaultValue = "fuzzy") String searchMode,
                       @RequestParam(value = "sortBy", required = false) String sortBy) {
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedMode = "exact".equalsIgnoreCase(searchMode) ? "exact" : "fuzzy";
        String keywordLikePattern = normalizedKeyword == null ? null : "%" + normalizedKeyword + "%";
        List<String> keywordPatterns = buildKeywordPatterns(normalizedKeyword);
        try {
            return Result.success(productMapper.getActiveProducts(category, normalizedKeyword, keywordLikePattern, keywordPatterns, normalizedMode, sortBy));
        } catch (Exception ex) {
            ex.printStackTrace();
            // 兜底：当订单统计相关 SQL 因历史库结构差异失败时，降级为基础商品列表
            return Result.success(productMapper.getActiveProductsWithoutOrderStats(category, normalizedKeyword, keywordLikePattern, keywordPatterns, normalizedMode, sortBy));
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String normalized = keyword.trim().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private List<String> buildKeywordPatterns(String keyword) {
        if (keyword == null || keyword.isBlank()) return List.of();
        Set<String> terms = new LinkedHashSet<>();
        terms.add(keyword);
        String compact = keyword.replaceAll("\\s+", "");
        switch (compact) {
            case "手机" -> terms.addAll(List.of("小米", "iPhone", "华为", "数码电子", "影像旗舰"));
            case "电脑", "笔记本" -> terms.addAll(List.of("MateBook", "笔记本", "轻薄本", "数码电子"));
            case "耳机" -> terms.addAll(List.of("耳机", "蓝牙", "数码电子"));
            case "图书", "书" -> terms.addAll(List.of("图书文创", "人类简史", "纸张", "印刷"));
            case "运动鞋", "鞋" -> terms.addAll(List.of("跑鞋", "Nike", "运动户外", "服饰鞋帽"));
            default -> { }
        }
        return terms.stream().map(this::buildFuzzyPattern).toList();
    }

    private String buildFuzzyPattern(String term) {
        String compact = term.chars()
                .filter(ch -> !Character.isWhitespace(ch))
                .mapToObj(ch -> String.valueOf((char) ch))
                .collect(Collectors.joining("%"));
        return "%" + compact + "%";
    }

    @GetMapping("/detail")
    @Cacheable(cacheNames = "product:detail", key = "#id")
    public Result detail(@RequestParam("id") Long id) {
        Product p = productMapper.selectDetailById(id);
        if (p == null) return Result.error("商品不存在");
        return Result.success(p);
    }

    @GetMapping("/category/list")
    @Cacheable(cacheNames = "product:category:list")
    public Result categoryList() {
        return Result.success(productMapper.listOnlineCategories());
    }

    /**
     * 2. 发布商品（管理员入口）
     */
    @PostMapping("/publish")
    @CacheEvict(cacheNames = {"product:list", "product:detail", "product:category:list"}, allEntries = true)
    public Result publish(@RequestBody Product product, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities() != null &&
                auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            return Result.error("仅管理员可上架商品，请前往后台操作");
        }
        // --- 核心逻辑：获取真实的发布者 ID ---

        // 尝试从拦截器存入的属性中取
        Object userIdAttr = request.getAttribute("currentUserId");
        Long realUserId = null;

        if (userIdAttr != null) {
            realUserId = Long.parseLong(userIdAttr.toString());
        } else {
            // 如果过滤器还没生效，我们从 Authorization 头里手动解析一次
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring("Bearer ".length()).trim();
                if (!token.isEmpty()) {
                    try {
                        // 使用你的 JwtUtils 解析出用户 ID
                        realUserId = JwtUtils.getUserId(token);
                    } catch (Exception e) {
                        return Result.error("登录已过期，请重新登录后再发布");
                    }
                }
            }
        }

        // 如果最后还是拿不到 ID，说明用户没登录
        if (realUserId == null) {
            return Result.error("发布失败：未能获取到登录用户信息");
        }

        // 绑定真实的用户 ID
        product.setSellerId(realUserId);

        // 补全其他默认状态
        product.setAuditStatus(1);   // 默认审核通过
        product.setPublishStatus(1); // 默认上架
        product.setStock(1);         // 默认库存 1

        try {
            productMapper.insertProduct(product);
            return Result.success("发布成功！");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发布失败，数据库写入异常");
        }
    }

    /**
     * 3. 图片上传
     */
    @PostMapping("/upload")
    public Result upload(@RequestParam(value = "file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("请选择图片");
        try {
            File directory = new File(uploadPath);
            if (!directory.exists()) directory.mkdirs();
            String ext = resolveImageExtension(file);
            if (ext == null) {
                return Result.error("仅支持 jpg/jpeg/png/webp/gif 图片");
            }

            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File dest = new File(directory, fileName);
            file.transferTo(dest);
            return Result.success("/images/" + fileName);
        } catch (IOException e) {
            return Result.error("文件保存失败");
        }
    }

    private String resolveImageExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            String rawExt = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (List.of("jpg", "jpeg", "png", "webp", "gif").contains(rawExt)) {
                return "jpeg".equals(rawExt) ? "jpg" : rawExt;
            }
            if ("jfif".equals(rawExt)) {
                return "jpg";
            }
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return null;
        }
        String mime = contentType.toLowerCase(Locale.ROOT);
        return switch (mime) {
            case "image/jpeg", "image/jpg", "image/pjpeg" -> "jpg";
            case "image/png", "image/x-png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }

    // --- 管理员接口 ---
    @GetMapping("/admin/all")
    public Result adminAll() { return Result.success(productMapper.getAllForAdmin()); }

    @PostMapping("/admin/audit")
    public Result audit(@RequestParam(value = "id") Long id, @RequestParam(value = "status") Integer status) {
        productMapper.updateAuditStatus(id, status);
        return Result.success("操作成功");
    }

    @PostMapping("/admin/status")
    @CacheEvict(cacheNames = {"product:list", "product:detail"}, allEntries = true)
    public Result updateStatus(@RequestParam(value = "id") Long id, @RequestParam(value = "status") Integer status) {
        productMapper.updateStatus(id, status);
        return Result.success("操作成功");
    }

    @PostMapping("/admin/delete")
    @CacheEvict(cacheNames = {"product:list", "product:detail"}, allEntries = true)
    public Result adminDelete(@RequestParam("id") Long id) {
        int n = productMapper.adminDeleteById(id);
        if (n == 0) return Result.error("商品不存在");
        return Result.success("删除成功");
    }

    @GetMapping("/admin/category/list")
    public Result adminCategoryList() {
        return Result.success(productCategoryMapper.adminList());
    }

    @PostMapping("/admin/category/save")
    @CacheEvict(cacheNames = {"product:category:list"}, allEntries = true)
    public Result adminCategorySave(@RequestBody com.jiejie.product.entity.ProductCategory body) {
        if (body.getName() == null || body.getName().trim().isEmpty()) return Result.error("分类名称不能为空");
        if (body.getSortNo() == null) body.setSortNo(100);
        if (body.getStatus() == null) body.setStatus(1);
        if (body.getId() == null) {
            productCategoryMapper.insert(body);
        } else {
            productCategoryMapper.update(body);
        }
        return Result.success("操作成功");
    }

    @PostMapping("/admin/category/delete")
    @CacheEvict(cacheNames = {"product:category:list"}, allEntries = true)
    public Result adminCategoryDelete(@RequestParam("id") Long id) {
        productCategoryMapper.delete(id);
        return Result.success("删除成功");
    }

    @PostMapping("/admin/save")
    @CacheEvict(cacheNames = {"product:list", "product:detail"}, allEntries = true)
    public Result adminSave(@RequestBody Product product, HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("currentUserId");
        if (userIdAttr == null) return Result.error("请先登录");
        if (product.getName() == null || product.getName().trim().isEmpty()) return Result.error("商品名称不能为空");
        if (product.getPrice() == null || product.getPrice().doubleValue() <= 0) return Result.error("价格必须大于0");
        if (product.getCategory() == null || product.getCategory().trim().isEmpty()) return Result.error("商品分类不能为空");
        if (product.getStock() == null || product.getStock() < 0) product.setStock(1);
        if (product.getAuditStatus() == null) product.setAuditStatus(1);
        if (product.getPublishStatus() == null) product.setPublishStatus(1);
        if (product.getIsSeckill() == null) product.setIsSeckill(0);
        if (product.getOriginalPrice() == null) product.setOriginalPrice(product.getPrice());
        if (product.getIsSeckill() == 0) {
            product.setSeckillPrice(null);
            product.setSeckillStartTime(null);
            product.setSeckillEndTime(null);
        }
        normalizeProductImages(product);
        if (product.getId() == null) {
            if (product.getSellerId() == null) {
                product.setSellerId(Long.parseLong(userIdAttr.toString()));
            }
            try {
                productMapper.insertProduct(product);
            } catch (Exception e) {
                e.printStackTrace();
                String detail = e.getMessage() == null ? "" : e.getMessage();
                return Result.error("保存失败：" + (detail.isBlank() ? "请检查图片字段长度或格式" : detail));
            }
            return Result.success("新增成功");
        }
        int n;
        try {
            n = productMapper.updateProductByAdmin(product);
        } catch (Exception e) {
            e.printStackTrace();
            String detail = e.getMessage() == null ? "" : e.getMessage();
            return Result.error("更新失败：" + (detail.isBlank() ? "请检查图片字段长度或格式" : detail));
        }
        if (n == 0) return Result.error("商品不存在或更新失败");
        return Result.success("更新成功");
    }

    private void normalizeProductImages(Product product) {
        List<String> images = new ArrayList<>();
        images.addAll(parseImageValues(product.getImageUrl()));
        images.addAll(parseImageValues(product.getImage()));
        // 去重并限制最多 9 张
        List<String> normalized = images.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .limit(9)
                .toList();
        if (normalized.isEmpty()) {
            product.setImage(null);
            product.setImageUrl(null);
            return;
        }
        product.setImage(normalized.get(0));
        product.setImageUrl(toLegacySafeJsonArray(normalized));
    }

    private List<String> parseImageValues(String input) {
        if (input == null || input.trim().isEmpty()) return List.of();
        String s = input.trim();
        // 兼容历史脏数据：去掉外层多余引号（例如 "\"[\\\"/images/a.jpg\\\"]\""）
        while ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            if (s.length() < 2) break;
            s = s.substring(1, s.length() - 1).trim();
        }
        if (!s.startsWith("[") || !s.endsWith("]")) return List.of(s);
        String body = s.substring(1, s.length() - 1).trim();
        if (body.isEmpty()) return List.of();
        String[] arr = body.split(",");
        List<String> out = new ArrayList<>();
        for (String item : arr) {
            String v = item.trim();
            if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
                v = v.substring(1, v.length() - 1);
            }
            v = v.replace("\\\"", "\"").replace("\\\\", "\\");
            while ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                if (v.length() < 2) break;
                v = v.substring(1, v.length() - 1).trim();
            }
            if (v.startsWith("[") && v.endsWith("]")) {
                out.addAll(parseImageValues(v));
                continue;
            }
            if (!v.isBlank()) out.add(v);
        }
        return out;
    }

    private String toJsonArray(List<String> images) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < images.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(images.get(i).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private String toLegacySafeJsonArray(List<String> images) {
        // 兼容旧库 image_url=VARCHAR(500)：尽可能保留多图，但不超过安全长度
        List<String> out = new ArrayList<>();
        for (String image : images) {
            out.add(image);
            String candidate = toJsonArray(out);
            if (candidate.length() > LEGACY_IMAGE_URL_SAFE_LEN) {
                out.remove(out.size() - 1);
                break;
            }
        }
        if (out.isEmpty()) out.add(images.get(0));
        return toJsonArray(out);
    }

    @PostMapping("/favorite/toggle")
    public Result toggleFavorite(HttpServletRequest request, @RequestParam("productId") Long productId) {
        Object userIdAttr = request.getAttribute("currentUserId");
        if (userIdAttr == null) {
            return Result.error("请先登录");
        }
        Long userId = Long.parseLong(userIdAttr.toString());
        int exists = productFavoriteMapper.exists(userId, productId);
        if (exists > 0) {
            productFavoriteMapper.delete(userId, productId);
            return Result.success(Map.of("favorited", false));
        }
        productFavoriteMapper.insert(userId, productId);
        return Result.success(Map.of("favorited", true));
    }

    @GetMapping("/favorite/list")
    public Result favoriteList(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("currentUserId");
        if (userIdAttr == null) {
            return Result.error("请先登录");
        }
        Long userId = Long.parseLong(userIdAttr.toString());
        return Result.success(productFavoriteMapper.listProductIds(userId));
    }

    @PostMapping("/review/add")
    public Result addReview(HttpServletRequest request, @RequestBody ProductReview review) {
        Object userIdAttr = request.getAttribute("currentUserId");
        if (userIdAttr == null) {
            return Result.error("请先登录");
        }
        if (review.getProductId() == null) {
            return Result.error("缺少商品ID");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            return Result.error("评分范围应为1-5");
        }
        if (review.getContent() == null || review.getContent().trim().isEmpty()) {
            return Result.error("评价内容不能为空");
        }
        review.setUserId(Long.parseLong(userIdAttr.toString()));
        review.setContent(review.getContent().trim());
        productReviewMapper.insert(review);
        return Result.success("评价成功");
    }

    @GetMapping("/review/list")
    public Result reviewList(@RequestParam("productId") Long productId) {
        return Result.success(productReviewMapper.listByProductId(productId));
    }

    @PostMapping("/review/delete")
    public Result deleteMyReview(HttpServletRequest request, @RequestParam("id") Long id) {
        Object userIdAttr = request.getAttribute("currentUserId");
        if (userIdAttr == null) {
            return Result.error("请先登录");
        }
        Long userId = Long.parseLong(userIdAttr.toString());
        int n = productReviewMapper.deleteByIdAndUser(id, userId);
        if (n <= 0) {
            return Result.error("只能删除自己的评价，或评价已不存在");
        }
        return Result.success("删除成功");
    }

    @GetMapping("/admin/review/list")
    public Result adminReviewList() {
        return Result.success(productReviewMapper.adminListAll());
    }

    @PostMapping("/admin/review/delete")
    public Result adminReviewDelete(@RequestParam("id") Long id) {
        int n = productReviewMapper.adminDeleteById(id);
        if (n == 0) return Result.error("评价不存在或已删除");
        return Result.success("删除成功");
    }
}
