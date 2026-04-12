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
    /** 0-待支付，1-已支付/待发货，2-已发货 */
    private Integer orderStatus;
    private Date createTime;

    // 辅助字段：连表查询，非 orders 表列
    private String productName;
    private String productImage;
    /** 商品卖家，用于卖家订单与私信权限 */
    private Long sellerId;
}