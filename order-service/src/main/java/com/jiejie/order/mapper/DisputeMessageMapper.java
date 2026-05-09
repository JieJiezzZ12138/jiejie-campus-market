package com.jiejie.order.mapper;

import com.jiejie.order.entity.DisputeMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DisputeMessageMapper {
    @Insert("INSERT INTO dispute_message(order_id, sender_id, sender_role, content, create_time) " +
            "VALUES(#{orderId}, #{senderId}, #{senderRole}, #{content}, NOW())")
    int insert(DisputeMessage x);

    @Select("SELECT * FROM dispute_message WHERE order_id = #{orderId} ORDER BY id ASC")
    List<DisputeMessage> listByOrderId(@Param("orderId") Long orderId);
}
