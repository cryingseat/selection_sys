package com.labselection.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一接口响应体：业务状态码 + 提示信息 + 数据体。
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(int code, String message, T data, long timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data, System.currentTimeMillis());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> message(String message, T data) {
        return new ApiResponse<>(0, message, data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}
