package com.jiejie.product.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ProductReview {
    private Long id;
    private Long productId;
    private Long userId;
    private Integer rating;
    private String content;
    private String imageUrl;
    private Date createTime;
    private String nickname;
    private String avatar;
}
