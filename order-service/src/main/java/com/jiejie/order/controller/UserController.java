package com.jiejie.order.controller;

import com.jiejie.common.Result;
import com.jiejie.order.entity.User;
import com.jiejie.order.entity.UserAddress;
import com.jiejie.order.entity.SystemNotice;
import com.jiejie.order.entity.BannerItem;
import com.jiejie.order.entity.AdminFeedback;
import com.jiejie.order.entity.UserFeedback;
import com.jiejie.order.mapper.AdminNotificationMapper;
import com.jiejie.order.mapper.UserAddressMapper;
import com.jiejie.order.mapper.SystemNoticeMapper;
import com.jiejie.order.mapper.BannerItemMapper;
import com.jiejie.order.mapper.UserMapper;
import com.jiejie.order.mapper.AdminFeedbackMapper;
import com.jiejie.order.mapper.UserFeedbackMapper;
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
    @Autowired
    private UserAddressMapper userAddressMapper;
    @Autowired
    private SystemNoticeMapper systemNoticeMapper;
    @Autowired
    private BannerItemMapper bannerItemMapper;
    @Autowired
    private AdminFeedbackMapper adminFeedbackMapper;
    @Autowired
    private UserFeedbackMapper userFeedbackMapper;

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
        if (StringUtils.hasText(phoneTrim) && !phoneTrim.matches("^1[3-9]\\d{9}$")) {
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
    public Result getAdminUserList(@RequestParam(value = "keyword", required = false) String keyword) {
        String k = keyword != null ? keyword.trim() : null;
        if (!StringUtils.hasText(k)) {
            return Result.success(userMapper.findAllForAdmin());
        }
        return Result.success(userMapper.findAllForAdminByKeyword(k));
    }

    @PostMapping("/admin/status")
    public Result updateUserStatus(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        userMapper.updateStatus(id, status);
        return Result.success("账号状态已更新");
    }

    @PostMapping("/admin/role")
    public Result updateUserRole(HttpServletRequest request, @RequestParam("id") Long id, @RequestParam("role") String role) {
        User admin = requireSuperAdminUser(request);
        if (admin == null) {
            return Result.error("需要超级管理员权限");
        }
        if (!"ADMIN".equals(role) && !"USER".equals(role) && !"SUPER_ADMIN".equals(role)) {
            return Result.error("无效角色");
        }
        userMapper.updateRole(id, role);
        return Result.success("权限设置已生效");
    }

    private User requireAdminUser(HttpServletRequest request) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) {
            return null;
        }
        User u = userMapper.findById(uid);
        if (u == null || (!"ADMIN".equals(u.getRole()) && !"SUPER_ADMIN".equals(u.getRole()))) {
            return null;
        }
        return u;
    }

    private User requireSuperAdminUser(HttpServletRequest request) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) return null;
        User u = userMapper.findById(uid);
        if (u == null || !"SUPER_ADMIN".equals(u.getRole())) {
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

    @GetMapping("/address/list")
    public Result addressList(HttpServletRequest request) {
        Long uid = AuthContext.currentUserId(request);
        return Result.success(userAddressMapper.listByUserId(uid));
    }

    @PostMapping("/address/save")
    public Result addressSave(HttpServletRequest request, @RequestBody UserAddress body) {
        Long uid = AuthContext.currentUserId(request);
        body.setUserId(uid);
        if (body.getIsDefault() != null && body.getIsDefault() == 1) {
            userAddressMapper.clearDefault(uid);
        }
        if (body.getId() == null) {
            userAddressMapper.insert(body);
        } else {
            userAddressMapper.update(body);
        }
        return Result.success("保存成功");
    }

    @PostMapping("/address/delete")
    public Result addressDelete(HttpServletRequest request, @RequestParam("id") Long id) {
        Long uid = AuthContext.currentUserId(request);
        userAddressMapper.delete(id, uid);
        return Result.success("删除成功");
    }

    @GetMapping("/notice/list")
    public Result noticeList(@RequestParam(value = "limit", defaultValue = "5") int limit) {
        int cap = Math.min(Math.max(limit, 1), 20);
        return Result.success(systemNoticeMapper.listOnline(cap));
    }

    @GetMapping("/banner/list")
    public Result bannerList() {
        return Result.success(bannerItemMapper.listOnline());
    }

    @GetMapping("/admin/notice/list")
    public Result adminNoticeList(HttpServletRequest request) {
        if (requireAdminUser(request) == null) return Result.error("需要管理员权限");
        return Result.success(systemNoticeMapper.adminList());
    }

    @PostMapping("/admin/notice/save")
    public Result adminNoticeSave(HttpServletRequest request, @RequestBody SystemNotice body) {
        if (requireSuperAdminUser(request) == null) return Result.error("需要超级管理员权限");
        if (body.getId() == null) systemNoticeMapper.insert(body);
        else systemNoticeMapper.update(body);
        return Result.success("操作成功");
    }

    @PostMapping("/admin/notice/delete")
    public Result adminNoticeDelete(HttpServletRequest request, @RequestParam("id") Long id) {
        if (requireSuperAdminUser(request) == null) return Result.error("需要超级管理员权限");
        systemNoticeMapper.delete(id);
        return Result.success("删除成功");
    }

    @GetMapping("/admin/banner/list")
    public Result adminBannerList(HttpServletRequest request) {
        if (requireAdminUser(request) == null) return Result.error("需要管理员权限");
        return Result.success(bannerItemMapper.adminList());
    }

    @PostMapping("/admin/banner/save")
    public Result adminBannerSave(HttpServletRequest request, @RequestBody BannerItem body) {
        if (requireSuperAdminUser(request) == null) return Result.error("需要超级管理员权限");
        if (body.getId() == null) bannerItemMapper.insert(body);
        else bannerItemMapper.update(body);
        return Result.success("操作成功");
    }

    @PostMapping("/admin/banner/delete")
    public Result adminBannerDelete(HttpServletRequest request, @RequestParam("id") Long id) {
        if (requireSuperAdminUser(request) == null) return Result.error("需要超级管理员权限");
        bannerItemMapper.delete(id);
        return Result.success("删除成功");
    }

    @PostMapping("/admin/feedback/submit")
    public Result submitAdminFeedback(HttpServletRequest request, @RequestBody Map<String, String> body) {
        User u = requireAdminUser(request);
        if (u == null) return Result.error("需要管理员权限");
        String content = body.get("content") != null ? body.get("content").trim() : "";
        if (!StringUtils.hasText(content)) return Result.error("请填写反馈内容");
        AdminFeedback f = new AdminFeedback();
        f.setReporterId(u.getId());
        f.setContent(content);
        adminFeedbackMapper.insert(f);
        return Result.success("已提交给超级管理员");
    }

    @GetMapping("/admin/feedback/mine")
    public Result myAdminFeedback(HttpServletRequest request) {
        User u = requireAdminUser(request);
        if (u == null) return Result.error("需要管理员权限");
        return Result.success(adminFeedbackMapper.listMine(u.getId()));
    }

    @GetMapping("/super-admin/feedback/list")
    public Result superFeedbackList(HttpServletRequest request) {
        if (requireSuperAdminUser(request) == null) return Result.error("需要超级管理员权限");
        return Result.success(adminFeedbackMapper.listAll());
    }

    @PostMapping("/super-admin/feedback/status")
    public Result superFeedbackStatus(HttpServletRequest request,
                                      @RequestParam("id") Long id,
                                      @RequestParam("status") Integer status) {
        if (requireSuperAdminUser(request) == null) return Result.error("需要超级管理员权限");
        if (status == null || (status != 0 && status != 1 && status != 2)) {
            return Result.error("无效状态");
        }
        adminFeedbackMapper.updateStatus(id, status);
        return Result.success("状态已更新");
    }

    @PostMapping("/feedback/submit")
    public Result submitUserFeedback(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) return Result.error("请先登录");
        String content = body.get("content") != null ? body.get("content").trim() : "";
        if (!StringUtils.hasText(content)) return Result.error("请填写反馈内容");
        UserFeedback f = new UserFeedback();
        f.setUserId(uid);
        f.setContent(content);
        userFeedbackMapper.insert(f);
        return Result.success("反馈已提交");
    }

    @GetMapping("/feedback/mine")
    public Result myFeedback(HttpServletRequest request) {
        Long uid = AuthContext.currentUserId(request);
        if (uid == null) return Result.error("请先登录");
        return Result.success(userFeedbackMapper.listMine(uid));
    }

    @GetMapping("/admin/feedback/user-list")
    public Result adminUserFeedbackList(HttpServletRequest request) {
        if (requireAdminUser(request) == null) return Result.error("需要管理员权限");
        return Result.success(userFeedbackMapper.listAll());
    }

    @PostMapping("/admin/feedback/user-status")
    public Result adminUserFeedbackStatus(HttpServletRequest request,
                                          @RequestParam("id") Long id,
                                          @RequestParam("status") Integer status,
                                          @RequestParam(value = "replyContent", required = false) String replyContent) {
        if (requireAdminUser(request) == null) return Result.error("需要管理员权限");
        if (status == null || (status != 0 && status != 1 && status != 2)) return Result.error("状态无效");
        userFeedbackMapper.updateStatus(id, status, replyContent != null ? replyContent.trim() : null);
        return Result.success("处理完成");
    }
}
