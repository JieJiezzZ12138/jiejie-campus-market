package com.jiejie.order.mapper;

import com.jiejie.order.entity.ChatInboxItem;
import com.jiejie.order.entity.ChatThread;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatThreadMapper {

    @Select("SELECT * FROM chat_thread WHERE id = #{id}")
    ChatThread findById(@Param("id") Long id);

    @Select("SELECT * FROM chat_thread WHERE product_id = #{productId} AND customer_id = #{customerId}")
    ChatThread findByProductAndCustomer(@Param("productId") Long productId, @Param("customerId") Long customerId);

    @Insert("INSERT INTO chat_thread (product_id, seller_id, customer_id, create_time) " +
            "VALUES (#{productId}, #{sellerId}, #{customerId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatThread row);

    @Select("SELECT ct.id AS threadId, ct.product_id AS productId, " +
            "CASE WHEN ct.seller_id = #{uid} THEN ct.customer_id ELSE ct.seller_id END AS peerUserId, " +
            "CASE WHEN ct.seller_id = #{uid} THEN uc.nickname ELSE us.nickname END AS peerNickname, " +
            "(SELECT pm.content FROM private_message pm WHERE pm.thread_id = ct.id ORDER BY pm.id DESC LIMIT 1) AS lastPreview, " +
            "(SELECT MAX(pm.create_time) FROM private_message pm WHERE pm.thread_id = ct.id) AS lastTime, " +
            "(SELECT COUNT(1) FROM private_message pm " +
            " LEFT JOIN chat_thread_read rr ON rr.thread_id = pm.thread_id AND rr.user_id = #{uid} " +
            " WHERE pm.thread_id = ct.id AND pm.receiver_id = #{uid} " +
            " AND (rr.last_read_time IS NULL OR pm.create_time > rr.last_read_time)) AS unreadCount " +
            "FROM chat_thread ct " +
            "LEFT JOIN sys_user us ON us.id = ct.seller_id " +
            "LEFT JOIN sys_user uc ON uc.id = ct.customer_id " +
            "WHERE ct.seller_id = #{uid} OR ct.customer_id = #{uid} " +
            "ORDER BY COALESCE((SELECT MAX(pm2.create_time) FROM private_message pm2 WHERE pm2.thread_id = ct.id), ct.create_time) DESC")
    List<ChatInboxItem> listInboxForUser(@Param("uid") Long uid);
}
