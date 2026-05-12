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
    /** 0-待支付，1-已支付/待发货，2-已发货，3-已完成 */
    private Integer orderStatus;
    private Date createTime;
    private Long addressId;
    private String receiver;
    private String receiverPhone;
    private String receiverAddress;

    // 订单快照字段与辅助展示字段
    private String productName;
    private String productImage;
    private Long sellerId;
    private String buyerAddress;
    private String sellerAddress;
    private String buyerNickname;
    private String buyerUsername;
    private String buyerPhone;
    private String sellerNickname;
    private String sellerUsername;
    private String sellerPhone;
    private String selectedSpec;
    private Long couponId;
    private String couponTitle;
    private BigDecimal discountAmount;
    private String paymentMethod;
    private Date payTime;
    private String payTxnNo;
}
