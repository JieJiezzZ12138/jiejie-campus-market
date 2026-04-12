package com.jiejie.order.mapper;

import com.jiejie.order.entity.AdminNotification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AdminNotificationMapper {

    @Insert("INSERT INTO admin_notification (order_id, sender_id, preview, is_read, create_time) " +
            "VALUES (#{orderId}, #{senderId}, #{preview}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(AdminNotification row);

    /** 仅统计「交易反馈」类通知（不再收录用户私信） */
    @Select("SELECT COUNT(*) FROM admin_notification WHERE is_read = 0 AND preview LIKE '[交易反馈]%'")
    int countUnread();

    @Select("SELECT * FROM admin_notification WHERE preview LIKE '[交易反馈]%' ORDER BY create_time DESC LIMIT #{limit}")
    List<AdminNotification> listRecent(@Param("limit") int limit);

    @Update("UPDATE admin_notification SET is_read = 1 WHERE id = #{id}")
    int markRead(@Param("id") Long id);

    @Update("UPDATE admin_notification SET is_read = 1 WHERE is_read = 0 AND preview LIKE '[交易反馈]%'")
    int markAllRead();
}
