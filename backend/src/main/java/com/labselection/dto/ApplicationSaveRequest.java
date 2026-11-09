package com.labselection.dto;

import com.labselection.domain.enums.AdjustmentPreference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ApplicationSaveRequest(
        @NotNull(message = "请选择意向导师")
        List<Long> mentorIds,

        @Size(max = 1000, message = "个人简介不能超过 1000 字")
        String intro,

        @Size(max = 1000, message = "技术基础不能超过 1000 字")
        String skills,

        @Size(max = 1000, message = "项目经历不能超过 1000 字")
        String projectExperience,

        @NotNull(message = "请选择调剂意愿")
        AdjustmentPreference adjustmentPreference,

        List<Long> excludedLabIds,

        Integer version
) {
}
