package com.jiejie.auth.controller;

// 👉 核心修改 1：导入 common 模块的统一下发标准
import com.jiejie.common.Result;
import com.jiejie.common.utils.JwtUtils;

import com.jiejie.auth.entity.SysUser;
import com.jiejie.auth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        // 1. 去数据库查这个用户
        SysUser user = userMapper.findByUsername(username);

        // 2. 校验密码和账号状态
        if (user != null && user.getPassword().equals(password)) {
            // 这里对应了你提到的痛点：管理员界面的用户审核封禁功能
            if (user.getAuditStatus() == 0) {
                return Result.error("您的账号已被管理员封禁，无法登录！");
            }
            if (user.getAuditStatus() == 2) {
                return Result.error("您的账号正在审核中，请耐心等待。");
            }

            // 3. 登录成功，生成大厂级别的 JWT Token！
            // 👉 核心修改 2：调用 common 模块的新版方法，只传 ID 和 用户名
            String token = JwtUtils.generateToken(user.getId(), user.getUsername());

            // 4. 把 Token 和用户信息返回给前端
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            // 为了安全，把密码抹除再传给前端
            user.setPassword(null);
            data.put("userInfo", user);

            return Result.success(data);
        }

        return Result.error("账号或密码错误，请重试！");
    }
}