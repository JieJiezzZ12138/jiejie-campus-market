package com.jiejie.product.entity;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;

    // 👉 核心：必须有这个字段，且名字必须是 category，对应数据库
    private String category;

    private BigDecimal price;
    private BigDecimal originalPrice;
    private String image;
    private String imageUrl;
    private Integer stock;
    private Long sellerId;
    private Integer auditStatus;
    private Integer publishStatus;
    private Date createTime;

    // 连表查询字段
    private String sellerName;
    private String sellerAvatar;
    private String sellerAddress;
    private Integer salesCount;
    private String specJson;
    private Integer isSeckill;
    private BigDecimal seckillPrice;
    private Date seckillStartTime;
    private Date seckillEndTime;
}
