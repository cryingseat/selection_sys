package com.labselection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplementDecisionRequest(
        @NotNull(message = "请选择补录意向记录") Long supplementIntentId,
        @NotBlank(message = "请选择初选结论") String decision,
        Integer waitlistRank,
        String comment,
        String reason
) {
}
