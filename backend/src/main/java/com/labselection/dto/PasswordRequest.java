package com.labselection.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordRequest(
        @NotBlank(message = "请输入新密码") String password
) {
}
