package com.jiejie.order.mapper;

import com.jiejie.order.entity.BannerItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BannerItemMapper {
    @Select("SELECT * FROM banner_item WHERE status = 1 ORDER BY sort_no ASC, id DESC")
    List<BannerItem> listOnline();

    @Select("SELECT * FROM banner_item ORDER BY sort_no ASC, id DESC")
    List<BannerItem> adminList();

    @Insert("INSERT INTO banner_item(title, subtitle, image_url, bg_color, sort_no, status, create_time) " +
            "VALUES(#{title}, #{subtitle}, #{imageUrl}, #{bgColor}, #{sortNo}, #{status}, NOW())")
    int insert(BannerItem x);

    @Update("UPDATE banner_item SET title=#{title}, subtitle=#{subtitle}, image_url=#{imageUrl}, bg_color=#{bgColor}, sort_no=#{sortNo}, status=#{status} WHERE id=#{id}")
    int update(BannerItem x);

    @Delete("DELETE FROM banner_item WHERE id=#{id}")
    int delete(@Param("id") Long id);
}
