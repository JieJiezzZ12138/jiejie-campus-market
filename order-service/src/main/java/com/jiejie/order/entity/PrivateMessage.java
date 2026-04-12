package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class PrivateMessage {
    private Long id;
    private Long orderId;
    private Long threadId;
    private Long productId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Date createTime;
}
