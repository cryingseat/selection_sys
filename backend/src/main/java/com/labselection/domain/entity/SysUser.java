package com.labselection.domain.entity;

import com.labselection.domain.enums.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysUser {
    private Long id;
    private String account;
    private String passwordHash;
    private String displayName;
    private Role role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
