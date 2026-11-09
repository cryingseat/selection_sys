package com.labselection.domain.entity;

import com.labselection.domain.enums.IntentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationIntent {
    private Long id;
    private Long applicationId;
    private Long batchId;
    private Long studentId;
    private Long mentorId;
    private Integer sortNo;
    private IntentStatus status;
    private Integer waitlistRank;
    private String comment;
    private Long decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private String mentorName;
    private String mentorTitle;
    private String mentorNo;
    private Long labId;
    private String labName;
    private String labCode;
    private String researchDirection;
    private Integer mentorQuota;
    private Integer labQuota;
    private String studentNo;
    private String studentName;
    private String major;
    private String className;
    private String grade;
    private String applicationIntro;
    private String applicationSkills;
    private String applicationProject;
}
