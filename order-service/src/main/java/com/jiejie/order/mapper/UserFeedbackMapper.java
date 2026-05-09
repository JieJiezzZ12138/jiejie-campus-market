package com.jiejie.order.mapper;

import com.jiejie.order.entity.UserFeedback;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserFeedbackMapper {
    @Insert("INSERT INTO user_feedback(user_id, content, status, create_time) VALUES(#{userId}, #{content}, 0, NOW())")
    int insert(UserFeedback x);

    @Select("SELECT * FROM user_feedback WHERE user_id = #{userId} ORDER BY id DESC")
    List<UserFeedback> listMine(@Param("userId") Long userId);

    @Select("SELECT * FROM user_feedback ORDER BY id DESC")
    List<UserFeedback> listAll();

    @Update("UPDATE user_feedback SET status=#{status}, reply_content=#{replyContent} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("replyContent") String replyContent);
}
