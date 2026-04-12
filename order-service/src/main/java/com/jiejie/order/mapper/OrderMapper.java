package com.jiejie.order.mapper;

import com.jiejie.order.entity.Order;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 1. 创建订单
     * 增加了对 pay_time 的处理（下单时为 null）
     */
    @Insert("INSERT INTO orders (order_no, buyer_id, product_id, buy_count, total_amount, order_status, create_time) " +
            "VALUES (#{orderNo}, #{buyerId}, #{productId}, #{buyCount}, #{totalAmount}, #{orderStatus}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void createOrder(Order order);

    /**
     * 2. 查询用户的订单列表 (带商品名称和主图)
     * 关联 product 表，这样前端列表能显示“商品名字”，非常重要！
     */
    @Select("SELECT o.*, p.name as productName, p.image as productImage, p.seller_id as sellerId, " +
            "bu.campus_address as buyerAddress, su.campus_address as sellerAddress, " +
            "bu.nickname as buyerNickname, bu.username as buyerUsername, bu.phone as buyerPhone, " +
            "su.nickname as sellerNickname, su.username as sellerUsername, su.phone as sellerPhone " +
            "FROM orders o " +
            "LEFT JOIN product p ON o.product_id = p.id " +
            "LEFT JOIN sys_user bu ON bu.id = o.buyer_id " +
            "LEFT JOIN sys_user su ON su.id = p.seller_id " +
            "WHERE o.buyer_id = #{userId} " +
            "ORDER BY o.create_time DESC")
    List<Order> findByUserId(@Param("userId") Long userId);

    @Select("SELECT o.*, p.name as productName, p.image as productImage, p.seller_id as sellerId, " +
            "bu.campus_address as buyerAddress, su.campus_address as sellerAddress, " +
            "bu.nickname as buyerNickname, bu.username as buyerUsername, bu.phone as buyerPhone, " +
            "su.nickname as sellerNickname, su.username as sellerUsername, su.phone as sellerPhone " +
            "FROM orders o " +
            "JOIN product p ON o.product_id = p.id " +
            "LEFT JOIN sys_user bu ON bu.id = o.buyer_id " +
            "LEFT JOIN sys_user su ON su.id = p.seller_id " +
            "WHERE p.seller_id = #{sellerId} " +
            "ORDER BY o.create_time DESC")
    List<Order> findBySellerId(@Param("sellerId") Long sellerId);

    @Select("SELECT o.*, p.name as productName, p.image as productImage, p.seller_id as sellerId, " +
            "bu.campus_address as buyerAddress, su.campus_address as sellerAddress, " +
            "bu.nickname as buyerNickname, bu.username as buyerUsername, bu.phone as buyerPhone, " +
            "su.nickname as sellerNickname, su.username as sellerUsername, su.phone as sellerPhone " +
            "FROM orders o " +
            "LEFT JOIN product p ON o.product_id = p.id " +
            "LEFT JOIN sys_user bu ON bu.id = o.buyer_id " +
            "LEFT JOIN sys_user su ON su.id = p.seller_id " +
            "WHERE o.id = #{id}")
    Order findByIdWithProduct(@Param("id") Long id);

    @Update("UPDATE orders SET order_status = 1, pay_time = NOW() " +
            "WHERE id = #{id} AND buyer_id = #{buyerId} AND order_status = 0")
    int markPaid(@Param("id") Long id, @Param("buyerId") Long buyerId);

    @Update("UPDATE orders SET order_status = 2 WHERE id = #{id} AND order_status = 1")
    int markShipped(@Param("id") Long id);

    @Update("UPDATE orders SET order_status = 3 WHERE id = #{id} AND buyer_id = #{buyerId} AND order_status = 2")
    int markReceived(@Param("id") Long id, @Param("buyerId") Long buyerId);

    /**
     * 4. 安全校验：根据订单 ID 和 用户 ID 查询
     * 用在支付前，确保“我只能支付我自己的订单”，防止黑客攻击
     */
    @Select("SELECT * FROM orders WHERE id = #{id} AND buyer_id = #{userId}")
    Order findByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 5. 根据订单编号查询 (后台管理或搜索用)
     */
    @Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Order findByOrderNo(@Param("orderNo") String orderNo);

    /** 同一商品、买家最近一笔订单（用于收件箱/会话补全 orderId，合并历史私信） */
    @Select("SELECT o.*, p.name as productName, p.image as productImage, p.seller_id as sellerId, " +
            "bu.campus_address as buyerAddress, su.campus_address as sellerAddress, " +
            "bu.nickname as buyerNickname, bu.username as buyerUsername, bu.phone as buyerPhone, " +
            "su.nickname as sellerNickname, su.username as sellerUsername, su.phone as sellerPhone " +
            "FROM orders o " +
            "LEFT JOIN product p ON o.product_id = p.id " +
            "LEFT JOIN sys_user bu ON bu.id = o.buyer_id " +
            "LEFT JOIN sys_user su ON su.id = p.seller_id " +
            "WHERE o.product_id = #{productId} AND o.buyer_id = #{buyerId} " +
            "ORDER BY o.id DESC LIMIT 1")
    Order findLatestByProductAndBuyer(@Param("productId") Long productId, @Param("buyerId") Long buyerId);
}
