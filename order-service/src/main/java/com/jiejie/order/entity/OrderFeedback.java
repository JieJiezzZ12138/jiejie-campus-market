package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class OrderFeedback {
    private Long id;
    private Long orderId;
    private Long userId;
    private String content;
    private Date createTime;
}
