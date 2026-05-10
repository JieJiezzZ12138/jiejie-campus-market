package com.jiejie.product.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ProductCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String icon;
    private Integer sortNo;
    private Integer status;
    private Date createTime;
}
