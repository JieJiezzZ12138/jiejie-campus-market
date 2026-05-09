package com.jiejie.order.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Coupon {
    private Long id;
    private String title;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private Integer stock;
    private Integer status;
    private Date startTime;
    private Date endTime;
    private Date createTime;
}
