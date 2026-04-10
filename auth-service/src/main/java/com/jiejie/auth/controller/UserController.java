package com.jiejie.auth.controller;

import com.jiejie.common.Result;
import com.jiejie.auth.entity.SysUser;
import com.jiejie.auth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/admin/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    // 获取所有用户列表
    @GetMapping("/list")
    public Result list() {
        List<SysUser> users = userMapper.findAll();
        // 为了安全，列表里不返回密码
        if (users != null) {
            users.forEach(u -> u.setPassword(null));
        }
        return Result.success(users);
    }

    // 审核/封禁用户 (status: 0-封禁, 1-正常)
    // 👉 重点：必须显式写明 @RequestParam("id") 和 @RequestParam("status")
    @PostMapping("/audit")
    public Result audit(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        userMapper.updateAuditStatus(id, status);
        String msg = (status == 1) ? "用户已解封" : "用户已封禁";
        return Result.success(msg);
    }
}