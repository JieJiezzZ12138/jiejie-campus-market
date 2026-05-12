package com.jiejie.product.dto;

import lombok.Data;

@Data
public class ProductSummaryResponse {
    private Long id;
    private String name;
    private Double price;
    private String image;
    private String imageUrl;
    private Long sellerId;
    private String sellerName;
    private Integer stock;
}
