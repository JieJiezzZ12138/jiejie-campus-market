package com.jiejie.order.mapper;

import com.jiejie.order.entity.OrderNotice;
import com.jiejie.order.entity.OrderNoticeItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderNoticeMapper {

    @Insert("INSERT INTO order_notice (order_id, user_id, scope, notice_type, is_read, create_time) " +
            "VALUES (#{orderId}, #{userId}, #{scope}, #{noticeType}, #{isRead}, NOW())")
    void insert(OrderNotice notice);

    @Select("SELECT COUNT(1) FROM order_notice WHERE user_id = #{userId} AND scope = #{scope} AND is_read = 0")
    int countUnreadByScope(@Param("userId") Long userId, @Param("scope") String scope);

    @Update("UPDATE order_notice SET is_read = 1 WHERE user_id = #{userId} AND order_id = #{orderId} AND is_read = 0")
    int markReadByOrder(@Param("userId") Long userId, @Param("orderId") Long orderId);

    @Update("UPDATE order_notice SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markReadById(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE order_notice SET is_read = 1 WHERE user_id = #{userId} AND scope = #{scope} AND is_read = 0")
    int markReadAll(@Param("userId") Long userId, @Param("scope") String scope);

    @Update("<script>" +
            "UPDATE order_notice SET is_read = 1 WHERE user_id = #{userId} AND scope = #{scope} AND is_read = 0 " +
            "AND notice_type IN " +
            "<foreach collection='types' item='t' open='(' separator=',' close=')'>#{t}</foreach>" +
            "</script>")
    int markReadByTypes(@Param("userId") Long userId, @Param("scope") String scope, @Param("types") List<String> types);

    @Select("SELECT n.id, n.order_id AS orderId, n.user_id AS userId, n.scope, " +
            "n.notice_type AS noticeType, n.is_read AS isRead, n.create_time AS createTime, " +
            "o.order_no AS orderNo, o.order_status AS orderStatus, " +
            "o.product_name AS productName, o.product_image AS productImage, " +
            "o.buyer_id AS buyerId, o.seller_id AS sellerId, " +
            "bu.nickname AS buyerNickname, su.nickname AS sellerNickname " +
            "FROM order_notice n " +
            "LEFT JOIN orders o ON o.id = n.order_id " +
            "LEFT JOIN sys_user bu ON bu.id = o.buyer_id " +
            "LEFT JOIN sys_user su ON su.id = o.seller_id " +
            "WHERE n.user_id = #{userId} AND n.scope = #{scope} " +
            "ORDER BY n.is_read ASC, n.create_time DESC " +
            "LIMIT #{limit}")
    List<OrderNoticeItem> listNotice(@Param("userId") Long userId,
                                     @Param("scope") String scope,
                                     @Param("limit") int limit);
}
