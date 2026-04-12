package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ChatInboxItem {
    private Long threadId;
    private Long productId;
    private String productName;
    private Long peerUserId;
    private String peerNickname;
    private String lastPreview;
    private Date lastTime;
    private Integer unreadCount;
}
