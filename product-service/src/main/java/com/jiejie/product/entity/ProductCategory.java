package com.jiejie.product.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ProductCategory {
    private Long id;
    private String name;
    private String icon;
    private Integer sortNo;
    private Integer status;
    private Date createTime;
}
