package com.labselection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustmentRequest(
        @NotNull(message = "请选择学生") Long studentId,
        @NotNull(message = "请选择目标导师") Long toMentorId,
        @NotBlank(message = "请填写调剂原因") String reason
) {
}
