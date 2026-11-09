package com.labselection.dto;

import com.labselection.domain.enums.InterviewConclusion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record InterviewRequest(
        @NotNull(message = "请选择报名学生") Long applicationId,
        LocalDateTime scheduledAt,
        String location,
        @Min(value = 0, message = "评分不能低于 0") @Max(value = 100, message = "评分不能超过 100")
        Integer score,
        String comment,
        InterviewConclusion conclusion
) {
}
