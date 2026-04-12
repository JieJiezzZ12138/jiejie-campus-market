package com.jiejie.order.entity;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;

    // 身份角色：比如 'ADMIN' (管理员) 或 'USER' (普通学生)
    private String role;

    // 账号状态：1-正常，0-封禁
    private Integer status;
    private String campusAddress;
}