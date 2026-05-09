package com.jiejie.auth.mapper;

import com.jiejie.auth.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface UserMapper {
    // 登录用
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(@Param("username") String username);

    @Select("SELECT * FROM sys_user WHERE phone = #{phone} LIMIT 1")
    SysUser findByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM sys_user WHERE email = #{email} LIMIT 1")
    SysUser findByEmail(@Param("email") String email);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(@Param("id") Long id);

    @Insert("INSERT INTO sys_user (username, password, nickname, avatar, phone, email, role, audit_status, campus_address, create_time) " +
            "VALUES (#{username}, #{password}, #{nickname}, #{avatar}, #{phone}, #{email}, #{role}, #{auditStatus}, #{campusAddress}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SysUser user);

    // 👉 管理员用：查询所有用户
    @Select("SELECT * FROM sys_user ORDER BY create_time DESC")
    List<SysUser> findAll();

    // 👉 管理员用：封禁或解封用户
    @Update("UPDATE sys_user SET audit_status = #{status} WHERE id = #{id}")
    void updateAuditStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
