package com.jiejie.order.mapper;

import com.jiejie.order.entity.AdminFeedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminFeedbackMapper {
    @Insert("INSERT INTO admin_feedback(reporter_id, content, status, create_time) VALUES(#{reporterId}, #{content}, 0, NOW())")
    int insert(AdminFeedback x);

    @Select("SELECT * FROM admin_feedback WHERE reporter_id = #{reporterId} ORDER BY id DESC")
    List<AdminFeedback> listMine(@Param("reporterId") Long reporterId);

    @Select("SELECT * FROM admin_feedback ORDER BY id DESC")
    List<AdminFeedback> listAll();

    @Update("UPDATE admin_feedback SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
}
