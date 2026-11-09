package com.labselection.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Student {
    private Long id;
    private Long userId;
    private String studentNo;
    private String name;
    private String gender;
    private String major;
    private String className;
    private String grade;
    private String phone;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 关联查询字段 */
    private String account;
    private Boolean enabled;
}
