package com.jiejie.order.entity;

import lombok.Data;

import java.util.Date;

@Data
public class OrderNoticeItem {
    private Long id;
    private Long orderId;
    private Long userId;
    private String scope;
    private String noticeType;
    private Integer isRead;
    private Date createTime;

    private String orderNo;
    private Integer orderStatus;
    private String productName;
    private String productImage;
    private Long buyerId;
    private Long sellerId;
    private String buyerNickname;
    private String sellerNickname;
}
