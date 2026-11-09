package com.labselection.domain.entity;

import com.labselection.domain.enums.LabStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Lab {
    private Long id;
    private String code;
    private String name;
    private String intro;
    private String direction;
    private String trainingPlan;
    private String requirements;
    private String contact;
    private Integer totalQuota;
    private LabStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询与统计字段 */
    private List<Mentor> mentors;
    private Integer mentorCount;
    private Integer applicantCount;
    private Integer confirmedCount;
    private Integer remainingQuota;
}
