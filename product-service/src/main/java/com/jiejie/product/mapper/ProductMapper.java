package com.jiejie.product.mapper;

import com.jiejie.product.entity.Product;
import com.jiejie.product.entity.ProductCategory;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("<script>" +
            "SELECT p.id, p.name, p.description, p.category, p.price, p.original_price as originalPrice, " +
            "p.image, p.image_url as imageUrl, p.stock, p.seller_id as sellerId, " +
            "p.spec_json as specJson, p.is_seckill as isSeckill, p.seckill_price as seckillPrice, p.seckill_start_time as seckillStartTime, p.seckill_end_time as seckillEndTime, " +
            "p.audit_status as auditStatus, p.publish_status as publishStatus, p.create_time, " +
            "u.nickname as sellerName, u.avatar as sellerAvatar, " +
            "u.campus_address as sellerAddress, " +
            "IFNULL((SELECT SUM(o.buy_count) FROM orders o WHERE o.product_id = p.id AND o.order_status IN (1,2,3)), 0) as salesCount " +
            "FROM product p " +
            "LEFT JOIN sys_user u ON p.seller_id = u.id " +
            "WHERE p.audit_status = 1 AND p.publish_status = 1 AND p.stock > 0 " +
            "<if test='category != null and category.toString() != \"\"'> AND p.category = #{category} </if> " +
            "<if test='keyword != null and keyword.toString() != \"\"'> AND (p.name LIKE CONCAT('%',#{keyword},'%') OR p.description LIKE CONCAT('%',#{keyword},'%')) </if> " +
            "<choose>" +
            "<when test='sortBy == \"price_asc\"'> ORDER BY p.price ASC, p.create_time DESC </when>" +
            "<when test='sortBy == \"price_desc\"'> ORDER BY p.price DESC, p.create_time DESC </when>" +
            "<when test='sortBy == \"sales_desc\"'> ORDER BY salesCount DESC, p.create_time DESC </when>" +
            "<otherwise> ORDER BY p.create_time DESC </otherwise>" +
            "</choose>" +
            "</script>")
    List<Product> getActiveProducts(@Param("category") String category, @Param("keyword") String keyword, @Param("sortBy") String sortBy);

    @Select("<script>" +
            "SELECT p.id, p.name, p.description, p.category, p.price, p.original_price as originalPrice, " +
            "p.image, p.image_url as imageUrl, p.stock, p.seller_id as sellerId, " +
            "p.spec_json as specJson, p.is_seckill as isSeckill, p.seckill_price as seckillPrice, p.seckill_start_time as seckillStartTime, p.seckill_end_time as seckillEndTime, " +
            "p.audit_status as auditStatus, p.publish_status as publishStatus, p.create_time, " +
            "u.nickname as sellerName, u.avatar as sellerAvatar, u.campus_address as sellerAddress, " +
            "0 as salesCount " +
            "FROM product p " +
            "LEFT JOIN sys_user u ON p.seller_id = u.id " +
            "WHERE p.audit_status = 1 AND p.publish_status = 1 AND p.stock > 0 " +
            "<if test='category != null and category.toString() != \"\"'> AND p.category = #{category} </if> " +
            "<if test='keyword != null and keyword.toString() != \"\"'> AND (p.name LIKE CONCAT('%',#{keyword},'%') OR p.description LIKE CONCAT('%',#{keyword},'%')) </if> " +
            "<choose>" +
            "<when test='sortBy == \"price_asc\"'> ORDER BY p.price ASC, p.create_time DESC </when>" +
            "<when test='sortBy == \"price_desc\"'> ORDER BY p.price DESC, p.create_time DESC </when>" +
            "<otherwise> ORDER BY p.create_time DESC </otherwise>" +
            "</choose>" +
            "</script>")
    List<Product> getActiveProductsWithoutOrderStats(@Param("category") String category, @Param("keyword") String keyword, @Param("sortBy") String sortBy);

    @Select("SELECT id, name, icon, sort_no as sortNo, status, create_time as createTime FROM product_category WHERE status=1 ORDER BY sort_no ASC, id ASC")
    List<ProductCategory> listOnlineCategories();

    @Select("SELECT p.id, p.name, p.description, p.category, p.price, p.original_price as originalPrice, " +
            "p.image, p.image_url as imageUrl, p.stock, p.seller_id as sellerId, " +
            "p.spec_json as specJson, p.is_seckill as isSeckill, p.seckill_price as seckillPrice, p.seckill_start_time as seckillStartTime, p.seckill_end_time as seckillEndTime, " +
            "p.audit_status as auditStatus, p.publish_status as publishStatus, p.create_time, " +
            "u.nickname as sellerName, u.avatar as sellerAvatar, " + // 👉 修复：这里也加了一个逗号
            "u.campus_address as sellerAddress " +
            "FROM product p LEFT JOIN sys_user u ON p.seller_id = u.id ORDER BY p.create_time DESC")
    List<Product> getAllForAdmin();

    @Insert("INSERT INTO product(name, description, category, price, original_price, image, image_url, stock, seller_id, spec_json, is_seckill, seckill_price, seckill_start_time, seckill_end_time, audit_status, publish_status, create_time) " +
            "VALUES(#{name}, #{description}, #{category}, #{price}, #{originalPrice}, #{image}, #{imageUrl}, #{stock}, #{sellerId}, #{specJson}, #{isSeckill}, #{seckillPrice}, #{seckillStartTime}, #{seckillEndTime}, #{auditStatus}, #{publishStatus}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertProduct(Product product);

    @Update("UPDATE product SET name=#{name}, description=#{description}, category=#{category}, price=#{price}, original_price=#{originalPrice}, " +
            "image=#{image}, image_url=#{imageUrl}, stock=#{stock}, spec_json=#{specJson}, is_seckill=#{isSeckill}, seckill_price=#{seckillPrice}, " +
            "seckill_start_time=#{seckillStartTime}, seckill_end_time=#{seckillEndTime}, audit_status=#{auditStatus}, publish_status=#{publishStatus} " +
            "WHERE id=#{id}")
    int updateProductByAdmin(Product product);

    @Update("UPDATE product SET audit_status = #{status} WHERE id = #{id}")
    void updateAuditStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE product SET publish_status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Delete("DELETE FROM product WHERE id = #{id}")
    int adminDeleteById(@Param("id") Long id);

    @Update("UPDATE product SET stock = stock - #{num} WHERE id = #{id} AND stock >= #{num}")
    int reduceStock(@Param("id") Long id, @Param("num") Integer num);

    @Select("SELECT COUNT(*) FROM product")
    int countTotal();

    @Select("SELECT COUNT(*) FROM product WHERE audit_status = 0")
    int countPending();

    @Select("SELECT COUNT(*) FROM product WHERE publish_status = 2 OR stock = 0")
    int countSoldOut();

    /** 按主键查询（含已售出/下架），用于消息与订单上下文 */
    @Select("SELECT p.id, p.name, p.description, p.category, p.price, p.original_price as originalPrice, " +
            "p.image, p.image_url as imageUrl, p.stock, p.seller_id as sellerId, " +
            "p.spec_json as specJson, p.is_seckill as isSeckill, p.seckill_price as seckillPrice, p.seckill_start_time as seckillStartTime, p.seckill_end_time as seckillEndTime, " +
            "p.audit_status as auditStatus, p.publish_status as publishStatus, p.create_time, " +
            "u.nickname as sellerName, u.avatar as sellerAvatar, u.campus_address as sellerAddress " +
            "FROM product p LEFT JOIN sys_user u ON p.seller_id = u.id WHERE p.id = #{id}")
    Product selectById(@Param("id") Long id);

    @Select("SELECT p.id, p.name, p.description, p.category, p.price, p.original_price as originalPrice, " +
            "p.image, p.image_url as imageUrl, p.stock, p.seller_id as sellerId, " +
            "p.spec_json as specJson, p.is_seckill as isSeckill, p.seckill_price as seckillPrice, p.seckill_start_time as seckillStartTime, p.seckill_end_time as seckillEndTime, " +
            "p.audit_status as auditStatus, p.publish_status as publishStatus, p.create_time, " +
            "u.nickname as sellerName, u.avatar as sellerAvatar, u.campus_address as sellerAddress, " +
            "IFNULL((SELECT SUM(o.buy_count) FROM orders o WHERE o.product_id = p.id AND o.order_status IN (1,2,3)), 0) as salesCount " +
            "FROM product p LEFT JOIN sys_user u ON p.seller_id = u.id WHERE p.id = #{id}")
    Product selectDetailById(@Param("id") Long id);
}
