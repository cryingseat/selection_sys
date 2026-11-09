package com.labselection.domain.entity;

import com.labselection.domain.enums.SupplementStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplementRound {
    private Long id;
    private Long batchId;
    private Integer roundNo;
    private String title;
    private SupplementStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 统计字段 */
    private Integer intentCount;
    private Integer offerCount;
    private Integer remainingQuota;
}
