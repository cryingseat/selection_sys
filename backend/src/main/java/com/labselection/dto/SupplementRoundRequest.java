package com.labselection.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record SupplementRoundRequest(
        @NotNull(message = "请填写补录轮次")
        @Min(value = 1, message = "轮次从 1 开始")
        @Max(value = 3, message = "最多支持三轮补录") Integer roundNo,
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
