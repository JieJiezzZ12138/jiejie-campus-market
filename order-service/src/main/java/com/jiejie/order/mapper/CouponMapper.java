package com.jiejie.order.mapper;

import com.jiejie.order.entity.Coupon;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CouponMapper {
    @Select("SELECT * FROM coupon WHERE status = 1 AND start_time <= NOW() AND end_time >= NOW() ORDER BY id DESC")
    List<Coupon> listOnline();

    @Select("SELECT * FROM coupon WHERE id = #{id}")
    Coupon findById(@Param("id") Long id);

    @Select("SELECT c.* FROM user_coupon uc JOIN coupon c ON uc.coupon_id = c.id " +
            "WHERE uc.user_id = #{userId} AND uc.use_status = 0 AND c.status = 1 AND c.end_time >= NOW() ORDER BY uc.id DESC")
    List<Coupon> listMyAvailable(@Param("userId") Long userId);

    @Insert("INSERT INTO user_coupon(user_id, coupon_id, use_status, create_time) VALUES(#{userId}, #{couponId}, 0, NOW())")
    int receive(@Param("userId") Long userId, @Param("couponId") Long couponId);

    @Update("UPDATE coupon SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int reduceStock(@Param("id") Long id);

    @Update("UPDATE user_coupon SET use_status = 1, use_time = NOW() WHERE user_id = #{userId} AND coupon_id = #{couponId} AND use_status = 0 LIMIT 1")
    int markUsed(@Param("userId") Long userId, @Param("couponId") Long couponId);
}
