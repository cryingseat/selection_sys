package com.labselection.domain.entity;

import com.labselection.domain.enums.AnnouncementStatus;
import com.labselection.domain.enums.AnnouncementType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Announcement {
    private Long id;
    private Long batchId;
    private String title;
    private AnnouncementType type;
    private String content;
    private AnnouncementStatus status;
    private LocalDateTime publishStart;
    private LocalDateTime publishEnd;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private String batchName;
    private String creatorName;
}
