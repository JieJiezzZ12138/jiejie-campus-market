package com.jiejie.product.mapper;

import com.jiejie.product.entity.Product;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("<script>" +
            "SELECT p.id, p.name, p.description, p.category, p.price, p.original_price as originalPrice, " +
            "p.image, p.image_url as imageUrl, p.stock, p.seller_id as sellerId, " +
            "p.audit_status as auditStatus, p.publish_status as publishStatus, p.create_time, " +
            "u.nickname as sellerName, u.avatar as sellerAvatar, " + // 👉 修复：这里加了一个逗号
            "u.campus_address as sellerAddress " +
            "FROM product p " +
            "LEFT JOIN sys_user u ON p.seller_id = u.id " +
            "WHERE p.audit_status = 1 AND p.publish_status = 1 AND p.stock > 0 " +
            "<if test='category != null and category.toString() != \"\"'> AND p.category = #{category} </if> " +
            "<if test='keyword != null and keyword.toString() != \"\"'> AND p.name LIKE CONCAT('%',#{keyword},'%') </if> " +
            "ORDER BY p.create_time DESC" +
            "</script>")
    List<Product> getActiveProducts(@Param("category") String category, @Param("keyword") String keyword);

    @Select("SELECT p.id, p.name, p.description, p.category, p.price, p.original_price as originalPrice, " +
            "p.image, p.image_url as imageUrl, p.stock, p.seller_id as sellerId, " +
            "p.audit_status as auditStatus, p.publish_status as publishStatus, p.create_time, " +
            "u.nickname as sellerName, u.avatar as sellerAvatar, " + // 👉 修复：这里也加了一个逗号
            "u.campus_address as sellerAddress " +
            "FROM product p LEFT JOIN sys_user u ON p.seller_id = u.id ORDER BY p.create_time DESC")
    List<Product> getAllForAdmin();

    @Insert("INSERT INTO product(name, description, category, price, original_price, image, image_url, stock, seller_id, audit_status, publish_status, create_time) " +
            "VALUES(#{name}, #{description}, #{category}, #{price}, #{originalPrice}, #{image}, #{imageUrl}, #{stock}, #{sellerId}, #{auditStatus}, #{publishStatus}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertProduct(Product product);

    @Update("UPDATE product SET audit_status = #{status} WHERE id = #{id}")
    void updateAuditStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE product SET publish_status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE product SET stock = stock - #{num} WHERE id = #{id} AND stock >= #{num}")
    int reduceStock(@Param("id") Long id, @Param("num") Integer num);

    @Select("SELECT COUNT(*) FROM product")
    int countTotal();

    @Select("SELECT COUNT(*) FROM product WHERE audit_status = 0")
    int countPending();

    @Select("SELECT COUNT(*) FROM product WHERE publish_status = 2 OR stock = 0")
    int countSoldOut();

    /** 按主键查询（含已售出/下架），用于私信与订单上下文 */
    @Select("SELECT p.id, p.name, p.description, p.category, p.price, p.original_price as originalPrice, " +
            "p.image, p.image_url as imageUrl, p.stock, p.seller_id as sellerId, " +
            "p.audit_status as auditStatus, p.publish_status as publishStatus, p.create_time, " +
            "u.nickname as sellerName, u.avatar as sellerAvatar, u.campus_address as sellerAddress " +
            "FROM product p LEFT JOIN sys_user u ON p.seller_id = u.id WHERE p.id = #{id}")
    Product selectById(@Param("id") Long id);
}