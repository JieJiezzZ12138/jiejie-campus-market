package com.jiejie.order.mapper;

import com.jiejie.order.entity.SystemNotice;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SystemNoticeMapper {
    @Select("SELECT * FROM system_notice WHERE status = 1 ORDER BY id DESC LIMIT #{limit}")
    List<SystemNotice> listOnline(@Param("limit") int limit);

    @Select("SELECT * FROM system_notice ORDER BY id DESC")
    List<SystemNotice> adminList();

    @Insert("INSERT INTO system_notice(title, content, status, create_time) VALUES(#{title}, #{content}, #{status}, NOW())")
    int insert(SystemNotice x);

    @Update("UPDATE system_notice SET title=#{title}, content=#{content}, status=#{status} WHERE id=#{id}")
    int update(SystemNotice x);

    @Delete("DELETE FROM system_notice WHERE id=#{id}")
    int delete(@Param("id") Long id);
}
