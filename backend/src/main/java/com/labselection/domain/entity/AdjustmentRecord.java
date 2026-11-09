package com.labselection.domain.entity;

import com.labselection.domain.enums.AssignmentSource;
import com.labselection.domain.enums.AssignmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdjustmentRecord {
    private Long id;
    private Long batchId;
    private Long studentId;
    private Long fromMentorId;
    private Long fromLabId;
    private Long toMentorId;
    private Long toLabId;
    private AssignmentSource type;
    private AssignmentStatus status;
    private String reason;
    private Long operatorId;
    private LocalDateTime createdAt;

    /** 关联查询字段 */
    private String studentNo;
    private String studentName;
    private String fromMentorName;
    private String fromLabName;
    private String toMentorName;
    private String toLabName;
    private String operatorName;
}
