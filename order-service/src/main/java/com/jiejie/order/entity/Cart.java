package com.jiejie.order.entity;

import lombok.Data;

@Data
public class Cart {
    private Long id;
    private String username;
    private Long productId;
    private Integer quantity;

    // 👈 必须手动加上这两个字段，用于接收数据库关联查询的结果
    private String productName;
    private Double price;
}