package com.jiejie.auth.controller;

// 👉 核心修改 1：导入 common 模块的统一下发标准
import com.jiejie.common.Result;
import com.jiejie.common.utils.JwtUtils;

import com.jiejie.auth.entity.SysUser;
import com.jiejie.auth.mapper.EmailVerifyCodeMapper;
import com.jiejie.auth.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EmailVerifyCodeMapper emailVerifyCodeMapper;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String username = params.get("username") != null ? params.get("username").trim() : "";
        String password = params.get("password");

        // 1. 支持账号/手机号/邮箱登录
        SysUser user = userMapper.findByUsername(username);
        if (user == null && username.matches("^1[3-9]\\d{9}$")) {
            user = userMapper.findByPhone(username);
        }
        if (user == null && username.contains("@")) {
            user = userMapper.findByEmail(username);
        }

        // 2. 校验密码和账号状态
        boolean passOk = false;
        boolean needUpgradeHash = false;
        if (user != null) {
            String dbPwd = user.getPassword() == null ? "" : user.getPassword();
            if (dbPwd.startsWith("$2a$") || dbPwd.startsWith("$2b$") || dbPwd.startsWith("$2y$")) {
                passOk = PASSWORD_ENCODER.matches(password, dbPwd);
            } else {
                // 兼容历史明文密码，登录成功后升级为 BCrypt
                passOk = dbPwd.equals(password);
                needUpgradeHash = passOk;
            }
        }

        if (user != null && passOk) {
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
            if (needUpgradeHash) {
                userMapper.updatePassword(user.getId(), PASSWORD_ENCODER.encode(password));
            }

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
        String phoneCode = body.get("phoneCode") != null ? body.get("phoneCode").trim() : "";
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
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式不正确");
        }
        if (org.springframework.util.StringUtils.hasText(email) && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return Result.error("邮箱格式不正确");
        }
        if (!org.springframework.util.StringUtils.hasText(phoneCode)) {
            return Result.error("请输入手机验证码");
        }
        if (userMapper.findByUsername(username) != null) {
            return Result.error("该账号已被注册");
        }
        SysUser byPhone = userMapper.findByPhone(phone);
        if (byPhone != null) {
            return Result.error("该手机号已被注册");
        }
        if (org.springframework.util.StringUtils.hasText(email)) {
            SysUser byEmail = userMapper.findByEmail(email);
            if (byEmail != null) {
                return Result.error("该邮箱已被注册");
            }
        }
        int valid = emailVerifyCodeMapper.countValid(phone, "REGISTER", phoneCode);
        if (valid <= 0) {
            return Result.error("手机验证码无效或已过期");
        }


        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PASSWORD_ENCODER.encode(password));
        user.setNickname(org.springframework.util.StringUtils.hasText(nickname) ? nickname.trim() : username);
        user.setAvatar(null);
        user.setPhone(phone);
        user.setEmail(email);
        user.setRole("USER");
        user.setAuditStatus(1);
        user.setCampusAddress(campusAddress != null ? campusAddress.trim() : null);

        userMapper.insert(user);
        emailVerifyCodeMapper.consume(phone, "REGISTER", phoneCode);

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
        String phone = body.get("phone") != null ? body.get("phone").trim() : "";
        String email = body.get("email") != null ? body.get("email").trim() : "";
        String mode = body.get("mode") != null ? body.get("mode").trim().toLowerCase() : "";
        String bizType = body.get("bizType") != null ? body.get("bizType").trim().toUpperCase() : "REGISTER";
        if (!"REGISTER".equals(bizType) && !"RESET_PASSWORD".equals(bizType)) {
            return Result.error("无效的业务类型");
        }
        if ("REGISTER".equals(bizType)) {
            if (!org.springframework.util.StringUtils.hasText(phone)) return Result.error("请输入手机号");
            if (!phone.matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
            SysUser byPhone = userMapper.findByPhone(phone);
            if (byPhone != null) {
                return Result.error("该手机号已被注册");
            }
            String code = String.format("%06d", new Random().nextInt(1000000));
            emailVerifyCodeMapper.insert(phone, bizType, code, 10);
            System.out.println("[JEMALL_SMS] send code to " + phone + ", biz=" + bizType + ", code=" + code);
            return Result.success("手机验证码已发送（10分钟有效）");
        }
        // RESET_PASSWORD: 按 mode 严格走手机号或邮箱分支
        if (!"phone".equals(mode) && !"email".equals(mode)) {
            return Result.error("找回方式无效");
        }
        String target = null;
        String channel = null;
        if ("phone".equals(mode)) {
            if (!org.springframework.util.StringUtils.hasText(phone)) return Result.error("请输入手机号");
            if (!phone.matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
            SysUser byPhone = userMapper.findByPhone(phone);
            if (byPhone == null) return Result.error("该手机号未注册");
            target = phone;
            channel = "SMS";
        } else if ("email".equals(mode)) {
            if (!org.springframework.util.StringUtils.hasText(email)) return Result.error("请输入邮箱");
            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return Result.error("邮箱格式不正确");
            SysUser byEmail = userMapper.findByEmail(email);
            if (byEmail == null) return Result.error("该邮箱未注册");
            target = email;
            channel = "MAIL";
        }
        if (target == null || channel == null) {
            return Result.error("找回方式无效");
        }
        String code = String.format("%06d", new Random().nextInt(1000000));
        emailVerifyCodeMapper.insert(target, bizType, code, 10);
        System.out.println("[JEMALL_" + channel + "] send code to " + target + ", biz=" + bizType + ", code=" + code);
        return Result.success((channel.equals("SMS") ? "手机" : "邮箱") + "验证码已发送（10分钟有效）");
    }

    @PostMapping("/reset-password")
    public Result resetPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username") != null ? body.get("username").trim() : "";
        String phone = body.get("phone") != null ? body.get("phone").trim() : "";
        String email = body.get("email") != null ? body.get("email").trim() : "";
        String mode = body.get("mode") != null ? body.get("mode").trim().toLowerCase() : "";
        String code = body.get("phoneCode") != null ? body.get("phoneCode").trim() : "";
        String newPassword = body.get("newPassword");
        if (!org.springframework.util.StringUtils.hasText(code)) {
            return Result.error("验证码不能为空");
        }
        if (!org.springframework.util.StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            return Result.error("新密码至少 6 位");
        }
        SysUser u = null;
        String target = null;
        if (!"phone".equals(mode) && !"email".equals(mode)) {
            return Result.error("找回方式无效");
        }
        if ("phone".equals(mode)) {
            if (!org.springframework.util.StringUtils.hasText(phone)) return Result.error("请输入手机号");
            if (!phone.matches("^1[3-9]\\d{9}$")) return Result.error("手机号格式不正确");
            u = userMapper.findByPhone(phone);
            if (u == null) return Result.error("该手机号未注册");
            if (org.springframework.util.StringUtils.hasText(username) && !username.equals(u.getUsername())) {
                return Result.error("手机号与账号不匹配");
            }
            target = phone;
        } else if ("email".equals(mode)) {
            if (!org.springframework.util.StringUtils.hasText(email)) return Result.error("请输入邮箱");
            if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return Result.error("邮箱格式不正确");
            u = userMapper.findByEmail(email);
            if (u == null) return Result.error("该邮箱未注册");
            if (org.springframework.util.StringUtils.hasText(username) && !username.equals(u.getUsername())) {
                return Result.error("邮箱与账号不匹配");
            }
            target = email;
        }
        if (u == null || target == null) {
            return Result.error("找回方式无效");
        }
        int valid = emailVerifyCodeMapper.countValid(target, "RESET_PASSWORD", code);
        if (valid <= 0) {
            return Result.error("验证码无效或已过期");
        }
        userMapper.updatePassword(u.getId(), PASSWORD_ENCODER.encode(newPassword));
        emailVerifyCodeMapper.consume(target, "RESET_PASSWORD", code);
        return Result.success("密码重置成功");
    }

    @PostMapping("/change-password")
    public Result changePassword(@RequestBody Map<String, String> body) {
        String username = body.get("username") != null ? body.get("username").trim() : "";
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (!org.springframework.util.StringUtils.hasText(username)) {
            return Result.error("缺少账号信息");
        }
        if (!org.springframework.util.StringUtils.hasText(oldPassword)) {
            return Result.error("请输入旧密码");
        }
        if (!org.springframework.util.StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            return Result.error("新密码至少 6 位");
        }
        SysUser u = userMapper.findByUsername(username);
        if (u == null) {
            return Result.error("用户不存在");
        }
        if (!PASSWORD_ENCODER.matches(oldPassword, u.getPassword())) {
            return Result.error("旧密码不正确");
        }
        userMapper.updatePassword(u.getId(), PASSWORD_ENCODER.encode(newPassword));
        return Result.success("密码修改成功");
    }

    // 专门用于测试网络是否通畅的接口
    @GetMapping("/ping")
    public Result ping() {
        return Result.success("ok");
    }
}
