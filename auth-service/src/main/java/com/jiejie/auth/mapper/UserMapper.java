package com.jiejie.auth.mapper;

import com.jiejie.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface UserMapper {
    // 登录用
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(@Param("username") String username);

    // 👉 管理员用：查询所有用户
    @Select("SELECT * FROM sys_user ORDER BY create_time DESC")
    List<SysUser> findAll();

    // 👉 管理员用：封禁或解封用户
    @Update("UPDATE sys_user SET audit_status = #{status} WHERE id = #{id}")
    void updateAuditStatus(@Param("id") Long id, @Param("status") Integer status);
}