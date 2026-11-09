package com.labselection.domain.entity;

import com.labselection.domain.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthToken {
    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private Boolean revoked;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;

    /** 登录态关联字段 */
    private String account;
    private String displayName;
    private Role role;
}
