package com.jiejie.auth.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface EmailVerifyCodeMapper {
    @Insert("INSERT INTO email_verify_code(email, biz_type, code, expire_time, used, create_time) " +
            "VALUES(#{email}, #{bizType}, #{code}, DATE_ADD(NOW(), INTERVAL #{ttlMinutes} MINUTE), 0, NOW())")
    int insert(@Param("email") String email, @Param("bizType") String bizType, @Param("code") String code, @Param("ttlMinutes") int ttlMinutes);

    @Select("SELECT COUNT(*) FROM email_verify_code " +
            "WHERE email=#{email} AND biz_type=#{bizType} AND code=#{code} AND used=0 AND expire_time > NOW()")
    int countValid(@Param("email") String email, @Param("bizType") String bizType, @Param("code") String code);

    @Update("UPDATE email_verify_code SET used=1 WHERE email=#{email} AND biz_type=#{bizType} AND code=#{code} AND used=0")
    int consume(@Param("email") String email, @Param("bizType") String bizType, @Param("code") String code);
}
