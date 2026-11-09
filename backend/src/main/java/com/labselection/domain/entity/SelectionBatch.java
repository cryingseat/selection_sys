package com.labselection.domain.entity;

import com.labselection.domain.enums.BatchStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SelectionBatch {
    private Long id;
    private String name;
    private String gradeScope;
    private String majorScope;
    private String description;
    private String rules;
    private BatchStatus status;
    private LocalDateTime lectureAt;
    private LocalDateTime openDayStart;
    private LocalDateTime openDayEnd;
    private LocalDateTime intentStart;
    private LocalDateTime intentEnd;
    private LocalDateTime interviewStart;
    private LocalDateTime interviewEnd;
    private LocalDateTime confirmStart;
    private LocalDateTime confirmEnd;
    private LocalDateTime supplementStart;
    private LocalDateTime supplementEnd;
    private LocalDateTime adjustStart;
    private LocalDateTime adjustEnd;
    private LocalDateTime publicityStart;
    private LocalDateTime publicityEnd;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 统计字段 */
    private Integer applicationCount;
    private Integer offerCount;
    private Integer admittedCount;
    private Integer labCount;
    private Integer mentorCount;
}
