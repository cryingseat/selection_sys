package com.labselection.domain.entity;

import com.labselection.domain.enums.SupplementIntentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplementIntent {
    private Long id;
    private Long roundId;
    private Long batchId;
    private Long studentId;
    private Long mentorId;
    private Integer sortNo;
    private SupplementIntentStatus status;
    private Integer waitlistRank;
    private String comment;
    private Long decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private String studentNo;
    private String studentName;
    private String major;
    private String className;
    private String mentorName;
    private String mentorTitle;
    private Long labId;
    private String labName;
    private Integer mentorQuota;
    private Integer labQuota;
    private String researchDirection;
    private Integer roundNo;
}
