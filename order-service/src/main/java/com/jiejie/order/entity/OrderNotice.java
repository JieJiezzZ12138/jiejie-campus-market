package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class OrderNotice {
    private Long id;
    private Long orderId;
    private Long userId;
    /** BUYER / SELLER */
    private String scope;
    /** NEW_ORDER / PAY_PENDING / PAID / SHIPPED / RECEIVED */
    private String noticeType;
    private Integer isRead;
    private Date createTime;
}
