package com.labselection.domain.entity;

import com.labselection.domain.enums.AssignmentSource;
import com.labselection.domain.enums.AssignmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FinalAssignment {
    private Long id;
    private Long batchId;
    private Long studentId;
    private Long applicationId;
    private Long mentorId;
    private Long labId;
    private AssignmentSource source;
    private AssignmentStatus status;
    private LocalDateTime confirmedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private String studentNo;
    private String studentName;
    private String major;
    private String className;
    private String mentorName;
    private String labName;
    private String mentorTitle;
    private String labDirection;
    private String labTraining;
    private String mentorDirection;
}
