package com.labselection.domain.entity;

import com.labselection.domain.enums.OfferSource;
import com.labselection.domain.enums.OfferStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Offer {
    private Long id;
    private Long batchId;
    private Long studentId;
    private Long mentorId;
    private Long labId;
    private Long applicationId;
    private Long intentId;
    private Long supplementIntentId;
    private OfferSource source;
    private OfferStatus status;
    private Integer roundNo;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private String studentNo;
    private String studentName;
    private String major;
    private String className;
    private String mentorName;
    private String labName;
}
