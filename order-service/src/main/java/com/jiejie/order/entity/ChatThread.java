package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatThread {
    private Long id;
    private Long productId;
    private Long sellerId;
    private Long customerId;
    private Date createTime;
}
