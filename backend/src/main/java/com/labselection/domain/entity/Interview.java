package com.labselection.domain.entity;

import com.labselection.domain.enums.InterviewConclusion;
import com.labselection.domain.enums.InterviewStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Interview {
    private Long id;
    private Long batchId;
    private Long applicationId;
    private Long labId;
    private Long mentorId;
    private LocalDateTime scheduledAt;
    private String location;
    private Integer score;
    private String comment;
    private InterviewConclusion conclusion;
    private InterviewStatus status;
    private Long recordedBy;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private String studentNo;
    private String studentName;
    private String major;
    private String className;
    private String labName;
    private String mentorName;
    private String applicationIntro;
    private String applicationSkills;
}
