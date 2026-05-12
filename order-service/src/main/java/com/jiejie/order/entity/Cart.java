package com.jiejie.order.entity;

import lombok.Data;

@Data
public class Cart {
    private Long id;
    private Long userId;
    private String username;
    private Long productId;
    private Integer quantity;

    // --- 关联商品表查出来的额外字段 ---
    private String productName;
    private Double price;

    // 👉 新增：用于接收商品图片的字段
    private String image;
    private String imageUrl;
    private String selectedSpec;
}
