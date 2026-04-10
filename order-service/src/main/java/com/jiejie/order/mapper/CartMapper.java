package com.jiejie.order.mapper;

import com.jiejie.order.entity.Cart;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CartMapper {

    /**
     * 1. 查询购物车列表
     * 👉 核心修复：把商品的 image 和 image_url 也查出来
     */
    @Select("SELECT c.id, c.username, c.product_id as productId, c.quantity, " +
            "p.name as productName, p.price, p.image, p.image_url as imageUrl " +
            "FROM cart c " +
            "LEFT JOIN product p ON c.product_id = p.id " +
            "WHERE c.username = #{username}")
    List<Cart> selectByUsername(@Param("username") String username);

    /**
     * 2. 检查商品是否已存在
     */
    @Select("SELECT id, username, product_id as productId, quantity " +
            "FROM cart WHERE username = #{username} AND product_id = #{productId} LIMIT 1")
    Cart findExistItem(@Param("username") String username, @Param("productId") Long productId);

    /**
     * 3. 插入记录
     */
    @Insert("INSERT INTO cart (username, product_id, quantity) " +
            "VALUES (#{username}, #{productId}, #{quantity})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Cart cart);

    /**
     * 4. 更新数量
     */
    @Update("UPDATE cart SET quantity = quantity + #{num} WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("num") Integer num);

    /**
     * 5. 根据主键 ID 删除
     */
    @Delete("DELETE FROM cart WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 6. 清空该用户的所有购物车
     */
    @Delete("DELETE FROM cart WHERE username = #{username}")
    int deleteByUsername(@Param("username") String username);

    /**
     * 7. 根据用户名和商品ID移除
     */
    @Delete("DELETE FROM cart WHERE username = #{username} AND product_id = #{productId}")
    int deleteByUsernameAndProductId(@Param("username") String username, @Param("productId") Long productId);
}