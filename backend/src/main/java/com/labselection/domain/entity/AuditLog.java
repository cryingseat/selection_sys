package com.labselection.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLog {
    private Long id;
    private Long batchId;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String reason;
    private String ip;
    private LocalDateTime createdAt;
}
