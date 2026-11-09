package com.labselection.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SupplementApplyRequest(
        @NotNull(message = "请选择补录轮次") Long roundId,
        @NotNull(message = "请选择意向导师")
        List<Long> mentorIds
) {
}
