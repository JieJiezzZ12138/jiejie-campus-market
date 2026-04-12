package com.jiejie.order.mapper;

import com.jiejie.order.entity.PrivateMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrivateMessageMapper {

    @Insert("INSERT INTO private_message (order_id, thread_id, product_id, sender_id, receiver_id, content, create_time) " +
            "VALUES (#{orderId}, #{threadId}, #{productId}, #{senderId}, #{receiverId}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(PrivateMessage message);

    @Select("SELECT * FROM private_message WHERE order_id = #{orderId} ORDER BY id ASC")
    List<PrivateMessage> listByOrderId(@Param("orderId") Long orderId);

    @Select("<script>" +
            "SELECT * FROM private_message WHERE thread_id = #{threadId} " +
            "<if test='legacyOrderId != null'> OR (order_id = #{legacyOrderId} AND thread_id IS NULL) </if> " +
            "ORDER BY id ASC" +
            "</script>")
    List<PrivateMessage> listForThread(@Param("threadId") Long threadId, @Param("legacyOrderId") Long legacyOrderId);
}
