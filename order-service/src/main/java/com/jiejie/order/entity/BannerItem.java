package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class BannerItem {
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String bgColor;
    private Integer sortNo;
    private Integer status;
    private Date createTime;
}
