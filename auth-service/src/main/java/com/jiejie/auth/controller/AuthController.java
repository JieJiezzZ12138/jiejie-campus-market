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
            // 👉 核心修改 2：调用 common 模块的新版方法，写入角色信息
            String role = user.getRole() != null ? user.getRole() : "USER";
            String token = JwtUtils.generateToken(user.getId(), user.getUsername(), role);

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

    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> body) {
        String username = body.get("username") != null ? body.get("username").trim() : "";
        String password = body.get("password");
        String nickname = body.get("nickname");
        String phone = body.get("phone") != null ? body.get("phone").trim() : "";
        String campusAddress = body.get("campusAddress");
        if (!org.springframework.util.StringUtils.hasText(username)) {
            return Result.error("请输入账号");
        }
        if (!org.springframework.util.StringUtils.hasText(password) || password.length() < 6) {
            return Result.error("密码至少 6 位");
        }
        if (!org.springframework.util.StringUtils.hasText(phone)) {
            return Result.error("请输入手机号");
        }
        if (!phone.matches("^\\d{7,20}$")) {
            return Result.error("手机号格式不正确");
        }
        if (userMapper.findByUsername(username) != null) {
            return Result.error("该账号已被注册");
        }
        SysUser byPhone = userMapper.findByPhone(phone);
        if (byPhone != null) {
            return Result.error("该手机号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(org.springframework.util.StringUtils.hasText(nickname) ? nickname.trim() : username);
        user.setAvatar(null);
        user.setPhone(phone);
        user.setRole("USER");
        user.setAuditStatus(1);
        user.setCampusAddress(campusAddress != null ? campusAddress.trim() : null);

        userMapper.insert(user);

        SysUser saved = userMapper.findById(user.getId());
        String token = JwtUtils.generateToken(saved.getId(), saved.getUsername(),
                saved.getRole() != null ? saved.getRole() : "USER");
        saved.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", saved);
        return Result.success(data);
    }
}
