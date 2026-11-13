package com.labselection.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class College {
    private Long id;
    private String code;
    private String name;
    private String intro;
    private String accentColor;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
