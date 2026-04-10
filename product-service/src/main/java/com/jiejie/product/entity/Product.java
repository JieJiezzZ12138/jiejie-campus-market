package com.jiejie.product.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Product {
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String imageUrl; // 真实图片URL
    private Integer stock;   // 真实库存
    private Long sellerId;   // 卖家ID
    private Integer auditStatus;  // 审核状态: 0-待审核, 1-通过, 2-拒绝
    private Integer publishStatus;// 上架状态: 0-下架, 1-上架
    private Date createTime;

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
    public Integer getPublishStatus() { return publishStatus; }
    public void setPublishStatus(Integer publishStatus) { this.publishStatus = publishStatus; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}