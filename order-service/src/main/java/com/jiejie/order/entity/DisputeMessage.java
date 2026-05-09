package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class DisputeMessage {
    private Long id;
    private Long orderId;
    private Long senderId;
    private String senderRole;
    private String content;
    private Date createTime;
}
