package com.jiejie.order.mapper;

import com.jiejie.order.entity.UserAddress;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserAddressMapper {
    @Select("SELECT * FROM user_address WHERE user_id = #{userId} ORDER BY is_default DESC, id DESC")
    List<UserAddress> listByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO user_address(user_id, receiver, phone, province, city, district, detail_address, is_default, create_time) " +
            "VALUES(#{userId}, #{receiver}, #{phone}, #{province}, #{city}, #{district}, #{detailAddress}, #{isDefault}, NOW())")
    int insert(UserAddress x);

    @Update("UPDATE user_address SET receiver=#{receiver}, phone=#{phone}, province=#{province}, city=#{city}, district=#{district}, detail_address=#{detailAddress}, is_default=#{isDefault} WHERE id=#{id} AND user_id=#{userId}")
    int update(UserAddress x);

    @Delete("DELETE FROM user_address WHERE id = #{id} AND user_id = #{userId}")
    int delete(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_address SET is_default = 0 WHERE user_id = #{userId}")
    int clearDefault(@Param("userId") Long userId);
}
