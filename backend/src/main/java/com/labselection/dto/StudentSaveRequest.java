package com.labselection.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentSaveRequest(
        String account,
        String password,
        @NotBlank(message = "学号不能为空") String studentNo,
        @NotBlank(message = "学生姓名不能为空") String name,
        String gender,
        String major,
        String className,
        String grade,
        String phone,
        String email
) {
}
