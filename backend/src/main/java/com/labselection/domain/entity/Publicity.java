package com.labselection.domain.entity;

import com.labselection.domain.enums.PublicityStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Publicity {
    private Long id;
    private Long batchId;
    private String title;
    private String content;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private PublicityStatus status;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private List<FinalAssignment> assignments;
    private Integer admittedCount;
    private String batchName;
}
