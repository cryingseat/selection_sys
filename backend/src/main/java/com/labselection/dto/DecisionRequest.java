package com.labselection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DecisionRequest(
        @NotNull(message = "请选择意向记录") Long intentId,
        @NotBlank(message = "请选择初选结论") String decision,
        Integer waitlistRank,
        String comment,
        String reason
) {
}
