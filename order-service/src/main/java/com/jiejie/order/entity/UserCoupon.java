package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class UserCoupon {
    private Long id;
    private Long userId;
    private Long couponId;
    private Integer useStatus;
    private Date useTime;
    private Date createTime;
}
