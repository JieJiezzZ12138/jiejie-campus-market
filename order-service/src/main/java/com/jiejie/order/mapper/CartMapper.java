package com.jiejie.order.mapper;

import com.jiejie.order.entity.Cart;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CartMapper {

    @Select("SELECT id, user_id as userId, username, product_id as productId, quantity, selected_spec as selectedSpec " +
            "FROM cart WHERE user_id = #{userId}")
    List<Cart> selectByUserId(@Param("userId") Long userId);

    /**
     * 2. 检查商品是否已存在
     */
    @Select("SELECT id, user_id as userId, username, product_id as productId, quantity, selected_spec as selectedSpec " +
            "FROM cart WHERE user_id = #{userId} AND product_id = #{productId} LIMIT 1")
    Cart findExistItem(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 3. 插入记录
     */
    @Insert("INSERT INTO cart (user_id, username, product_id, quantity, selected_spec) " +
            "VALUES (#{userId}, #{username}, #{productId}, #{quantity}, #{selectedSpec})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Cart cart);

    /**
     * 4. 更新数量
     */
    @Update("UPDATE cart SET quantity = quantity + #{num} WHERE id = #{id}")
    int updateQuantity(@Param("id") Long id, @Param("num") Integer num);

    @Update("UPDATE cart SET quantity = #{quantity}, username = #{username} WHERE user_id = #{userId} AND product_id = #{productId}")
    int setQuantity(@Param("userId") Long userId,
                    @Param("username") String username,
                    @Param("productId") Long productId,
                    @Param("quantity") Integer quantity);

    @Update("UPDATE cart SET selected_spec = #{selectedSpec}, username = #{username} WHERE user_id = #{userId} AND product_id = #{productId}")
    int setSpec(@Param("userId") Long userId,
                @Param("username") String username,
                @Param("productId") Long productId,
                @Param("selectedSpec") String selectedSpec);

    /**
     * 5. 根据主键 ID 删除
     */
    @Delete("DELETE FROM cart WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 6. 清空该用户的所有购物车
     */
    @Delete("DELETE FROM cart WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 7. 根据用户名和商品ID移除
     */
    @Delete("DELETE FROM cart WHERE user_id = #{userId} AND product_id = #{productId}")
    int deleteByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
