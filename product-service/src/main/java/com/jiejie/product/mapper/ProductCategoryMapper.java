package com.jiejie.product.mapper;

import com.jiejie.product.entity.ProductCategory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductCategoryMapper {
    @Select("SELECT id, name, icon, sort_no as sortNo, status, create_time as createTime FROM product_category WHERE status=1 ORDER BY sort_no ASC, id ASC")
    List<ProductCategory> listOnline();

    @Select("SELECT id, name, icon, sort_no as sortNo, status, create_time as createTime FROM product_category ORDER BY sort_no ASC, id ASC")
    List<ProductCategory> adminList();

    @Insert("INSERT INTO product_category(name, icon, sort_no, status, create_time) VALUES(#{name}, #{icon}, #{sortNo}, #{status}, NOW())")
    int insert(ProductCategory x);

    @Update("UPDATE product_category SET name=#{name}, icon=#{icon}, sort_no=#{sortNo}, status=#{status} WHERE id=#{id}")
    int update(ProductCategory x);

    @Delete("DELETE FROM product_category WHERE id=#{id}")
    int delete(@Param("id") Long id);
}
