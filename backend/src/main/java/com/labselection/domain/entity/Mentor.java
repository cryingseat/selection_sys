package com.labselection.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Mentor {
    private Long id;
    private Long userId;
    private String mentorNo;
    private String name;
    private String title;
    private Long labId;
    private String researchDirection;
    private String profile;
    private Integer quota;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询与统计字段 */
    private String account;
    private String labName;
    private String labCode;
    private Integer confirmedCount;
    private Integer offeredCount;
    private Integer applicantCount;
}
