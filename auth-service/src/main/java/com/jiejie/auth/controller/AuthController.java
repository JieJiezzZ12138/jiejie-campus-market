package com.jiejie.auth.controller;

// 👉 核心修改 1：导入 common 模块的统一下发标准
import com.jiejie.common.Result;
import com.jiejie.common.utils.JwtUtils;

import com.jiejie.auth.entity.SysUser;
import com.jiejie.auth.mapper.EmailVerifyCodeMapper;
import com.jiejie.auth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmailVerifyCodeMapper emailVerifyCodeMapper;

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
        String email = body.get("email") != null ? body.get("email").trim() : "";
        String emailCode = body.get("emailCode") != null ? body.get("emailCode").trim() : "";
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
        if (!org.springframework.util.StringUtils.hasText(email)) {
            return Result.error("请输入邮箱");
        }
        if (!phone.matches("^\\d{7,20}$")) {
            return Result.error("手机号格式不正确");
        }
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return Result.error("邮箱格式不正确");
        }
        if (!org.springframework.util.StringUtils.hasText(emailCode)) {
            return Result.error("请输入邮箱验证码");
        }
        if (userMapper.findByUsername(username) != null) {
            return Result.error("该账号已被注册");
        }
        SysUser byPhone = userMapper.findByPhone(phone);
        if (byPhone != null) {
            return Result.error("该手机号已被注册");
        }
        SysUser byEmail = userMapper.findByEmail(email);
        if (byEmail != null) {
            return Result.error("该邮箱已被注册");
        }
        int valid = emailVerifyCodeMapper.countValid(email, "REGISTER", emailCode);
        if (valid <= 0) {
            return Result.error("邮箱验证码无效或已过期");
        }


        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(org.springframework.util.StringUtils.hasText(nickname) ? nickname.trim() : username);
        user.setAvatar(null);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole("USER");
        user.setAuditStatus(1);
        user.setCampusAddress(campusAddress != null ? campusAddress.trim() : null);

        userMapper.insert(user);
        emailVerifyCodeMapper.consume(email, "REGISTER", emailCode);

        SysUser saved = userMapper.findById(user.getId());
        String token = JwtUtils.generateToken(saved.getId(), saved.getUsername(),
                saved.getRole() != null ? saved.getRole() : "USER");
        saved.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", saved);
        return Result.success(data);
    }

    @PostMapping("/email/send-code")
    public Result sendEmailCode(@RequestBody Map<String, String> body) {
        String email = body.get("email") != null ? body.get("email").trim() : "";
        String bizType = body.get("bizType") != null ? body.get("bizType").trim().toUpperCase() : "REGISTER";
        if (!org.springframework.util.StringUtils.hasText(email)) {
            return Result.error("请输入邮箱");
        }
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return Result.error("邮箱格式不正确");
        }
        if (!"REGISTER".equals(bizType) && !"RESET_PASSWORD".equals(bizType)) {
            return Result.error("无效的业务类型");
        }
        String code = String.format("%06d", new Random().nextInt(1000000));
        emailVerifyCodeMapper.insert(email, bizType, code, 10);
        // 课程作业场景：先以日志模拟发送
        System.out.println("[JEMALL_MAIL] send code to " + email + ", biz=" + bizType + ", code=" + code);
        return Result.success("验证码已发送（10分钟有效）");
    }

    @PostMapping("/reset-password")
    public Result resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email") != null ? body.get("email").trim() : "";
        String code = body.get("emailCode") != null ? body.get("emailCode").trim() : "";
        String newPassword = body.get("newPassword");
        if (!org.springframework.util.StringUtils.hasText(email) || !org.springframework.util.StringUtils.hasText(code)) {
            return Result.error("邮箱和验证码不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            return Result.error("新密码至少 6 位");
        }
        SysUser u = userMapper.findByEmail(email);
        if (u == null) {
            return Result.error("该邮箱未注册");
        }
        int valid = emailVerifyCodeMapper.countValid(email, "RESET_PASSWORD", code);
        if (valid <= 0) {
            return Result.error("邮箱验证码无效或已过期");
        }
        userMapper.updatePassword(u.getId(), newPassword);
        emailVerifyCodeMapper.consume(email, "RESET_PASSWORD", code);
        return Result.success("密码重置成功");
    }

    // 专门用于测试网络是否通畅的接口
    @GetMapping("/ping")
    public Result ping() {
        return Result.success("ok");
    }
}
