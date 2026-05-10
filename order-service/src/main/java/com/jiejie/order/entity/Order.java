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

    // 辅助字段：连表查询，非 orders 表列
    private String productName;
    private String productImage;
    /** 商品商家，用于商家订单与消息权限 */
    private Long sellerId;
    /** 买家与商家地址（订单中心展示） */
    private String buyerAddress;
    private String sellerAddress;
    /** 买卖双方昵称 / 账号（订单中心展示） */
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
