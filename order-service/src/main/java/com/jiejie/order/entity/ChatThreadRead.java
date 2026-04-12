package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatThreadRead {
    private Long threadId;
    private Long userId;
    private Date lastReadTime;
}
