package com.labselection.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MentorSaveRequest(
        String account,
        String password,
        @NotBlank(message = "导师姓名不能为空") String name,
        String mentorNo,
        String title,
        @NotNull(message = "请选择所属实训室") Long labId,
        String researchDirection,
        String profile,
        @NotNull(message = "请填写招生名额")
        @Min(value = 0, message = "招生名额不能为负数")
        @Max(value = 99, message = "招生名额过大") Integer quota,
        Boolean enabled
) {
}
