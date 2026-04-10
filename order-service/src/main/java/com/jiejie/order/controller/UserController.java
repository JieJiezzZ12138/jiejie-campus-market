package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.common.utils.JwtUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/login")
    public Result login(@RequestParam("username") String username,
                        @RequestParam("password") String password) {

        // 完美的登录逻辑模拟
        if ("student".equals(username) && "123456".equals(password)) {
            Long userId = 2L;
            String token = JwtUtils.generateToken(userId, username);
            return Result.success(token);
        }
        return Result.error("用户名或密码错误！");
    }
}