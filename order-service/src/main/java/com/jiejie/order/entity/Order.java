package com.jiejie.order.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private Long buyerId;
    private Long productId;
    private Integer buyCount;
    private BigDecimal totalAmount;
    private Integer orderStatus; // 0-待支付/已下单，1-已支付
    private Date createTime;

    // 辅助字段：用于在订单列表显示商品名称，数据库 orders 表里不需要这一列
    private String productName;
}