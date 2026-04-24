package com.research.workbench.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class SysUser extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 128, unique = true)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(nullable = false)
    private Integer status = 1;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode = "USER";

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
