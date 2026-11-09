package com.labselection.dto;

import jakarta.validation.constraints.NotBlank;

public record StageRequest(
        @NotBlank(message = "请选择目标阶段") String status,
        String reason
) {
}
