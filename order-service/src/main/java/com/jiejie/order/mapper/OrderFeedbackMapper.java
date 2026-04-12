package com.jiejie.order.mapper;

import com.jiejie.order.entity.OrderFeedback;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderFeedbackMapper {

    @Insert("INSERT INTO order_feedback (order_id, user_id, content, create_time) " +
            "VALUES (#{orderId}, #{userId}, #{content}, NOW())")
    void insert(@Param("orderId") Long orderId, @Param("userId") Long userId, @Param("content") String content);

    @Select("SELECT id, order_id AS orderId, user_id AS userId, content, create_time AS createTime " +
            "FROM order_feedback WHERE order_id = #{orderId} ORDER BY id ASC")
    List<OrderFeedback> listByOrderId(@Param("orderId") Long orderId);
}
