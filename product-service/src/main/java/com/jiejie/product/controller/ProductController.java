package com.jiejie.product.controller;

import com.jiejie.common.Result;
import com.jiejie.common.utils.JwtUtils;
import com.jiejie.product.entity.Product;
import com.jiejie.product.mapper.ProductMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    /**
     * 1. 查询商品列表
     */
    @GetMapping("/list")
    public Result list(@RequestParam(value = "category", required = false) String category,
                       @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(productMapper.getActiveProducts(category, keyword));
    }

    /**
     * 2. 发布商品 (👉 动态获取发布人 ID，告别“学霸小杰”)
     */
    @PostMapping("/publish")
    public Result publish(@RequestBody Product product, HttpServletRequest request) {
        // --- 核心逻辑：获取真实的发布者 ID ---

        // 尝试从拦截器存入的属性中取
        Object userIdAttr = request.getAttribute("currentUserId");
        Long realUserId = null;

        if (userIdAttr != null) {
            realUserId = Long.parseLong(userIdAttr.toString());
        } else {
            // 如果拦截器还没生效，我们从 Header 的 token 里手动解析一次
            String token = request.getHeader("token");
            if (token != null && !token.isEmpty()) {
                try {
                    // 使用你的 JwtUtils 解析出用户 ID
                    realUserId = JwtUtils.getUserId(token);
                } catch (Exception e) {
                    return Result.error("登录已过期，请重新登录后再发布");
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
        String realUploadPath = "/Users/jiejie/Desktop/web/my-system-cloud/pictures/";
        try {
            File directory = new File(realUploadPath);
            if (!directory.exists()) directory.mkdirs();
            String fileName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
            File dest = new File(directory, fileName);
            file.transferTo(dest);
            return Result.success("/images/" + fileName);
        } catch (IOException e) {
            return Result.error("文件保存失败");
        }
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
    public Result updateStatus(@RequestParam(value = "id") Long id, @RequestParam(value = "status") Integer status) {
        productMapper.updateStatus(id, status);
        return Result.success("操作成功");
    }
}