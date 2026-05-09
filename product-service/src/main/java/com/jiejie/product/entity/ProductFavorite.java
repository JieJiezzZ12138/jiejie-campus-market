package com.jiejie.product.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ProductFavorite {
    private Long id;
    private Long userId;
    private Long productId;
    private Date createTime;
}
