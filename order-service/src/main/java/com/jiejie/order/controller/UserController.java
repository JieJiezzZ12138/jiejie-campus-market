package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.entity.User;
import com.jiejie.order.mapper.AdminNotificationMapper;
import com.jiejie.order.mapper.UserMapper;
import com.jiejie.order.security.AuthContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AdminNotificationMapper adminNotificationMapper;

    @GetMapping("/profile")
    public Result getProfile(HttpServletRequest request) {
        Long userId = AuthContext.currentUserId(request);
        User user = userMapper.findById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @PutMapping("/profile")
    public Result updateProfile(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = AuthContext.currentUserId(request);
        User existing = userMapper.findById(userId);
        if (existing == null) {
            return Result.error("用户不存在");
        }
        String nickname = body.get("nickname");
        String avatar = body.get("avatar");
        String phone = body.get("phone");
        String campusAddress = body.get("campusAddress");
        if (!StringUtils.hasText(nickname)) {
            return Result.error("昵称不能为空");
        }
        String phoneTrim = phone != null ? phone.trim() : "";
        if (StringUtils.hasText(phoneTrim) && !phoneTrim.matches("^\\d{7,20}$")) {
            return Result.error("手机号格式不正确");
        }
        if (StringUtils.hasText(phoneTrim)) {
            User existingPhone = userMapper.findByPhone(phoneTrim);
            if (existingPhone != null && !existingPhone.getId().equals(userId)) {
                return Result.error("该手机号已被其他账号绑定");
            }
        }
        userMapper.updateProfile(userId, nickname.trim(),
                StringUtils.hasText(avatar) ? avatar.trim() : null,
                StringUtils.hasText(phoneTrim) ? phoneTrim : null,
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
        Long uid = AuthContext.currentUserId(request);
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
