package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.common.utils.JwtUtils;
import com.jiejie.order.entity.User;
import com.jiejie.order.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    /**
     * 1. 真实登录接口 (👉 修复 400 错误：改为 @RequestBody 接收 JSON)
     */
    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        // 1. 去数据库真实查询
        User user = userMapper.findByUsername(username);

        // 2. 校验账号密码
        if (user == null || !user.getPassword().equals(password)) {
            return Result.error("用户名或密码错误！");
        }

        // 3. 校验状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error("您的账号因违规已被封禁，请联系管理员！");
        }

        // 4. 生成 Token
        String token = JwtUtils.generateToken(user.getId(), user.getUsername());

        // 5. 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);

        return Result.success(data);
    }

    @GetMapping("/admin/list")
    public Result getAdminUserList() {
        return Result.success(userMapper.findAllForAdmin());
    }

    @PostMapping("/admin/status")
    public Result updateUserStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        userMapper.updateStatus(id, status);
        return Result.success("账号状态已更新");
    }

    @PostMapping("/admin/role")
    public Result updateUserRole(@RequestParam("id") Long id, @RequestParam("role") String role) {
        userMapper.updateRole(id, role);
        return Result.success("权限设置已生效");
    }
}