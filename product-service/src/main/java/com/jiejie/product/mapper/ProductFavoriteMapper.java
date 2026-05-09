package com.jiejie.product.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductFavoriteMapper {

    @Insert("INSERT INTO product_favorite(user_id, product_id, create_time) VALUES(#{userId}, #{productId}, NOW())")
    int insert(@Param("userId") Long userId, @Param("productId") Long productId);

    @Delete("DELETE FROM product_favorite WHERE user_id = #{userId} AND product_id = #{productId}")
    int delete(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("SELECT COUNT(*) FROM product_favorite WHERE user_id = #{userId} AND product_id = #{productId}")
    int exists(@Param("userId") Long userId, @Param("productId") Long productId);

    @Select("SELECT product_id FROM product_favorite WHERE user_id = #{userId}")
    List<Long> listProductIds(@Param("userId") Long userId);
}
