package com.jiejie.order.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;

@Mapper
public interface ChatThreadReadMapper {

    @Insert("INSERT INTO chat_thread_read (thread_id, user_id, last_read_time) " +
            "VALUES (#{threadId}, #{userId}, #{lastReadTime}) " +
            "ON DUPLICATE KEY UPDATE last_read_time = VALUES(last_read_time)")
    void upsert(@Param("threadId") Long threadId,
                @Param("userId") Long userId,
                @Param("lastReadTime") Date lastReadTime);

    @Select("SELECT COUNT(1) FROM private_message pm " +
            "LEFT JOIN chat_thread_read r ON r.thread_id = pm.thread_id AND r.user_id = #{uid} " +
            "WHERE pm.thread_id IS NOT NULL AND pm.receiver_id = #{uid} " +
            "AND (r.last_read_time IS NULL OR pm.create_time > r.last_read_time)")
    int countUnread(@Param("uid") Long uid);
}
