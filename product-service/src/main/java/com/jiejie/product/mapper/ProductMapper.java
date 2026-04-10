package com.jiejie.product.mapper;

import com.jiejie.product.entity.Product;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProductMapper {

    /**
     * 1. 前台展示查询
     * 逻辑：必须是审核通过(1) 且 处于上架状态(1) 且 库存大于0
     * 结算后的商品 publish_status 会变成 2，从而自动从这里消失
     */
    @Select("SELECT id, name, price, original_price as originalPrice, image_url as imageUrl, stock, seller_id as sellerId, audit_status as auditStatus, publish_status as publishStatus, create_time " +
            "FROM product " +
            "WHERE audit_status = 1 AND publish_status = 1 AND stock > 0 " +
            "ORDER BY create_time DESC")
    List<Product> getActiveProducts();

    /**
     * 2. 后台管理查询：获取全量数据
     */
    @Select("SELECT id, name, price, original_price as originalPrice, image_url as imageUrl, stock, seller_id as sellerId, audit_status as auditStatus, publish_status as publishStatus, create_time " +
            "FROM product ORDER BY create_time DESC")
    List<Product> getAllForAdmin();

    /**
     * 3. 插入商品
     * 初始状态：audit_status = 0 (待审核), publish_status = 1 (已上架)
     */
    @Insert("INSERT INTO product(name, price, original_price, image_url, stock, seller_id, audit_status, publish_status, create_time) " +
            "VALUES(#{name}, #{price}, #{originalPrice}, #{imageUrl}, #{stock}, #{sellerId}, 0, 1, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertProduct(Product product);

    /**
     * 4. 更新审核状态
     */
    @Update("UPDATE product SET audit_status = #{status} WHERE id = #{id}")
    void updateAuditStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 5. 更新上下架/售出状态 (修复“自动下架”报错的关键)
     * 在 OrderController 结算时调用：updateStatus(id, 2)
     */
    @Update("UPDATE product SET publish_status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 6. 扣减库存 (可选：如果你结算时想扣减库存数量)
     */
    @Update("UPDATE product SET stock = stock - #{num} WHERE id = #{id} AND stock >= #{num}")
    int reduceStock(@Param("id") Long id, @Param("num") Integer num);

    /**
     * 7. 大盘统计
     */
    @Select("SELECT COUNT(*) FROM product")
    int countTotal();

    @Select("SELECT COUNT(*) FROM product WHERE audit_status = 0")
    int countPending();

    @Select("SELECT COUNT(*) FROM product WHERE publish_status = 2 OR stock = 0")
    int countSoldOut();
}