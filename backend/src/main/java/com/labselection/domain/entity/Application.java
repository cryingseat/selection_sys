package com.labselection.domain.entity;

import com.labselection.domain.enums.AdjustmentPreference;
import com.labselection.domain.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Application {
    private Long id;
    private Long batchId;
    private Long studentId;
    private Long collegeId;
    private String intro;
    private String skills;
    private String projectExperience;
    private AdjustmentPreference adjustmentPreference;
    private String excludedLabIds;
    private ApplicationStatus status;
    private Integer version;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private String studentNo;
    private String studentName;
    private String major;
    private String className;
    private String grade;
    private String phone;
    private String email;
    private String batchName;
    private String batchStatus;
    private String collegeName;
    private List<ApplicationIntent> intents;
    private List<Long> excludedLabIdList;
}
