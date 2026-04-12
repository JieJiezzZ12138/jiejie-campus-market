package com.jiejie.auth.entity;

public class SysUser {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String role;
    private String avatar;
    private String phone;
    private Integer auditStatus; // 0-封禁, 1-正常, 2-待审核

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getAuditStatus() { return auditStatus; }
    public void setAuditStatus(Integer auditStatus) { this.auditStatus = auditStatus; }
}
