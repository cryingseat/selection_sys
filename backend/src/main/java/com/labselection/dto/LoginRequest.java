package com.labselection.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank(message = "请输入账号") String account,
        @NotBlank(message = "请输入密码") String password,
        @NotNull(message = "请选择学院") Long collegeId
) {
}
