package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class AdminNotification {
    private Long id;
    private Long orderId;
    private Long senderId;
    private String preview;
    private Integer isRead;
    private Date createTime;
}
