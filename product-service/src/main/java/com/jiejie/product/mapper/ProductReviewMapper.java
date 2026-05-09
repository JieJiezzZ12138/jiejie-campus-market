package com.jiejie.product.mapper;

import com.jiejie.product.entity.ProductReview;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductReviewMapper {

    @Insert("INSERT INTO product_review(product_id, user_id, rating, content, image_url, create_time) " +
            "VALUES(#{productId}, #{userId}, #{rating}, #{content}, #{imageUrl}, NOW())")
    int insert(ProductReview review);

    @Select("SELECT r.id, r.product_id as productId, r.user_id as userId, r.rating, r.content, r.image_url as imageUrl, r.create_time, " +
            "u.nickname, u.avatar " +
            "FROM product_review r LEFT JOIN sys_user u ON r.user_id = u.id " +
            "WHERE r.product_id = #{productId} ORDER BY r.create_time DESC")
    List<ProductReview> listByProductId(@Param("productId") Long productId);
}
