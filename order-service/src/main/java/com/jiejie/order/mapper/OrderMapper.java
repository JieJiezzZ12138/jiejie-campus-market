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
    @Select("SELECT o.*, p.name as productName, p.image as productImage " +
            "FROM orders o " +
            "LEFT JOIN product p ON o.product_id = p.id " +
            "WHERE o.buyer_id = #{userId} " +
            "ORDER BY o.create_time DESC")
    List<Order> findByUserId(@Param("userId") Long userId);

    /**
     * 3. 模拟支付：更新订单状态并记录支付时间
     * 逻辑：当状态变为 1 (已支付) 时，自动更新 pay_time 为当前时间
     */
    @Update("UPDATE orders SET order_status = #{status}, pay_time = NOW() " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

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
}