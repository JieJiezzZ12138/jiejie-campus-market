package com.jiejie.product.controller;

import com.jiejie.common.Result;
import com.jiejie.product.entity.Product;
import com.jiejie.product.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 商品管理控制器 (最终优化版)
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    // 文件上传路径：优先从配置文件读取，没有则使用默认
    @Value("${file.upload-path:/Users/jiejie/Downloads/uploads/}")
    private String uploadPath;

    // 1. 前台展示：获取“已审核(1)”且“上架中(1)”的商品
    // 逻辑：结算后的商品状态会变为 2，自动从该列表中消失
    @GetMapping("/list")
    public Result list() {
        return Result.success(productMapper.getActiveProducts());
    }

    // 2. 学生发布商品
    @PostMapping("/publish")
    public Result publish(@RequestBody Product p) {
        if (p.getPrice() == null || p.getName() == null) {
            return Result.error("商品名称或价格不能为空");
        }

        // 设置默认值
        p.setStock(p.getStock() == null ? 1 : p.getStock());
        p.setOriginalPrice(p.getOriginalPrice() == null ? p.getPrice() : p.getOriginalPrice());

        // 初始状态逻辑：
        p.setAuditStatus(0);   // 0-待审核
        p.setPublishStatus(1); // 1-已上架（审核通过后才会真正显示）

        // TODO: 后期集成登录后，从 Session/Token 获取实际 ID
        if (p.getSellerId() == null) {
            p.setSellerId(2L);
        }

        productMapper.insertProduct(p);
        return Result.success("商品发布成功，请等待管理员审核");
    }

    // 3. 管理员：获取所有商品（管理后台用）
    @GetMapping("/admin/all")
    public Result adminAll() {
        return Result.success(productMapper.getAllForAdmin());
    }

    // 4. 管理员：审核操作
    @PostMapping("/admin/audit")
    public Result audit(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        if (id == null || status == null) {
            return Result.error("参数错误");
        }
        productMapper.updateAuditStatus(id, status);
        return Result.success("审核状态已更新");
    }

    // 5. 管理员：手动上下架接口
    // 对接 Mapper 中的 updateStatus 方法，状态 1-上架，2-下架/售出
    @PostMapping("/admin/status")
    public Result updateStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        productMapper.updateStatus(id, status);
        return Result.success("上下架状态已更新");
    }

    // 6. 图片上传接口
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        try {
            File directory = new File(uploadPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

            File dest = new File(directory, fileName);
            file.transferTo(dest);

            // 返回虚拟路径，由 WebConfig 映射到本地磁盘
            return Result.success("/images/" + fileName);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }

    // 7. 管理端：大盘数据统计
    @GetMapping("/admin/stats")
    public Result stats() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", productMapper.countTotal());      // 总商品数
        map.put("pending", productMapper.countPending());  // 待审核数
        map.put("soldOut", productMapper.countSoldOut());  // 已售出/下架数
        return Result.success(map);
    }
}