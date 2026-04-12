package com.jiejie.order.mapper;

import com.jiejie.order.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface UserMapper {

    /**
     * 1. 真实登录查询：根据账号找用户
     * 👉 修复：把数据库的 audit_status 映射给实体类的 status 字段
     */
    @Select("SELECT id, username, password, nickname, avatar, role, audit_status as status, campus_address " +
            "FROM sys_user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, nickname, avatar, role, audit_status as status, campus_address " +
            "FROM sys_user WHERE id = #{id}")
    User findById(@Param("id") Long id);

    /**
     * 2. 管理员大盘：获取所有用户
     * 👉 修复：同样映射 audit_status
     */
    @Select("SELECT id, username, nickname, avatar, role, audit_status as status, campus_address " +
            "FROM sys_user ORDER BY id DESC")
    List<User> findAllForAdmin();

    @Insert("INSERT INTO sys_user (username, password, nickname, avatar, role, audit_status, campus_address, create_time) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{avatar}, #{role}, #{status}, #{campusAddress}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    @Update("UPDATE sys_user SET nickname = #{nickname}, avatar = #{avatar}, campus_address = #{campusAddress} WHERE id = #{id}")
    void updateProfile(@Param("id") Long id,
                       @Param("nickname") String nickname,
                       @Param("avatar") String avatar,
                       @Param("campusAddress") String campusAddress);

    /**
     * 3. 管理员操作：封号 / 解封
     * 👉 修复：更新真实的数据库字段 audit_status
     */
    @Update("UPDATE sys_user SET audit_status = #{status} WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 4. 管理员操作：提拔为管理员 / 降级为普通用户
     */
    @Update("UPDATE sys_user SET role = #{role} WHERE id = #{id}")
    void updateRole(@Param("id") Long id, @Param("role") String role);
}