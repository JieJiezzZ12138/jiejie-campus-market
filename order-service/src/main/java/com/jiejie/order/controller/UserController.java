package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.common.utils.JwtUtils;
import com.jiejie.order.entity.User;
import com.jiejie.order.mapper.AdminNotificationMapper;
import com.jiejie.order.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminNotificationMapper adminNotificationMapper;

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

        // 5. 返回结果（不向前端返回密码）
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", user);

        return Result.success(data);
    }

    /**
     * 用户注册：默认普通用户、审核通过后可立即登录
     */
    @PostMapping("/register")
    public Result register(@RequestBody Map<String, String> body) {
        String username = body.get("username") != null ? body.get("username").trim() : "";
        String password = body.get("password");
        String nickname = body.get("nickname");
        if (!StringUtils.hasText(username)) {
            return Result.error("请输入账号");
        }
        if (!StringUtils.hasText(password) || password.length() < 6) {
            return Result.error("密码至少 6 位");
        }
        if (userMapper.findByUsername(username) != null) {
            return Result.error("该账号已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : username);
        user.setAvatar(null);
        user.setRole("USER");
        user.setStatus(1);
        user.setCampusAddress(body.get("campusAddress") != null ? body.get("campusAddress").trim() : null);

        userMapper.insert(user);

        User saved = userMapper.findById(user.getId());
        String token = JwtUtils.generateToken(saved.getId(), saved.getUsername());
        saved.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", saved);
        return Result.success(data);
    }

    @GetMapping("/profile")
    public Result getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result updateProfile(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        User existing = userMapper.findById(userId);
        if (existing == null) {
            return Result.error("用户不存在");
        }
        String nickname = body.get("nickname");
        String avatar = body.get("avatar");
        String campusAddress = body.get("campusAddress");
        if (!StringUtils.hasText(nickname)) {
            return Result.error("昵称不能为空");
        }
        userMapper.updateProfile(userId, nickname.trim(),
                StringUtils.hasText(avatar) ? avatar.trim() : null,
                StringUtils.hasText(campusAddress) ? campusAddress.trim() : null);
        User fresh = userMapper.findById(userId);
        fresh.setPassword(null);
        return Result.success(fresh);
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

    private User requireAdminUser(HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("currentUserId");
        if (uid == null) {
            return null;
        }
        User u = userMapper.findById(uid);
        if (u == null || !"ADMIN".equals(u.getRole())) {
            return null;
        }
        return u;
    }

    @GetMapping("/admin/notifications/unread-count")
    public Result adminNotificationUnread(HttpServletRequest request) {
        if (requireAdminUser(request) == null) {
            return Result.error("需要管理员权限");
        }
        return Result.success(adminNotificationMapper.countUnread());
    }

    @GetMapping("/admin/notifications")
    public Result adminNotificationList(HttpServletRequest request,
                                         @RequestParam(value = "limit", defaultValue = "50") int limit) {
        if (requireAdminUser(request) == null) {
            return Result.error("需要管理员权限");
        }
        int cap = Math.min(Math.max(limit, 1), 200);
        return Result.success(adminNotificationMapper.listRecent(cap));
    }

    @PostMapping("/admin/notifications/read")
    public Result adminNotificationRead(HttpServletRequest request, @RequestParam("id") Long id) {
        if (requireAdminUser(request) == null) {
            return Result.error("需要管理员权限");
        }
        adminNotificationMapper.markRead(id);
        return Result.success("已标记已读");
    }

    @PostMapping("/admin/notifications/read-all")
    public Result adminNotificationReadAll(HttpServletRequest request) {
        if (requireAdminUser(request) == null) {
            return Result.error("需要管理员权限");
        }
        adminNotificationMapper.markAllRead();
        return Result.success("已全部标记已读");
    }
}