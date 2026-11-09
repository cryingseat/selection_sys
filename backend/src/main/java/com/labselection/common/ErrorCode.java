package com.labselection.common;

import org.springframework.http.HttpStatus;

/**
 * 可区分的业务错误码。前端依据 code 做精确提示与页面跳转。
 */
public enum ErrorCode {

    SUCCESS(0, "成功", HttpStatus.OK),

    PARAM_ERROR(40001, "请求参数不合法", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40101, "请先登录或登录已过期", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40301, "没有权限执行该操作", HttpStatus.FORBIDDEN),
    NOT_FOUND(40401, "请求的数据不存在", HttpStatus.NOT_FOUND),

    STAGE_NOT_ALLOWED(40010, "当前选拔阶段不允许该操作", HttpStatus.BAD_REQUEST),
    TIME_WINDOW_CLOSED(40011, "不在规定的时间窗口内", HttpStatus.BAD_REQUEST),
    NOT_ELIGIBLE(40012, "你不符合本批次的报名资格", HttpStatus.BAD_REQUEST),
    INTENT_LIMIT(40013, "最多只能填报 3 个平行意向", HttpStatus.BAD_REQUEST),
    DUPLICATE_INTENT(40014, "不能重复选择同一导师", HttpStatus.BAD_REQUEST),
    QUOTA_EXCEEDED(40015, "招生名额不足，无法提交该操作", HttpStatus.BAD_REQUEST),
    INTERVIEW_REQUIRED(40016, "该学生尚未完成面试考核", HttpStatus.BAD_REQUEST),
    OFFER_CONFLICT(40017, "该学生已确认其他实训室，无法重复录取", HttpStatus.BAD_REQUEST),
    CONFIRM_CONFLICT(40018, "拟录取确认状态冲突，请刷新后重试", HttpStatus.BAD_REQUEST),
    STATE_INVALID(40019, "当前数据状态不允许该操作", HttpStatus.BAD_REQUEST),
    BATCH_LOCKED(40020, "公示已发布或结果已锁定，请先撤回公示", HttpStatus.BAD_REQUEST),
    ADJUST_NOT_ALLOWED(40021, "学生未授权该类调剂，或目标实训室已被拒绝", HttpStatus.BAD_REQUEST),
    DEADLINE_EXPIRED(40022, "已超过确认截止时间，系统已按放弃处理", HttpStatus.BAD_REQUEST),
    DUPLICATE_APPLICATION(40023, "本批次已提交报名，请勿重复提交", HttpStatus.BAD_REQUEST),
    OFFER_NOT_FOUND(40024, "拟录取记录不存在或已失效", HttpStatus.BAD_REQUEST),
    SUPPLEMENT_CLOSED(40025, "补录轮次未开放或已结束", HttpStatus.BAD_REQUEST),
    ACCOUNT_DISABLED(40026, "账号已被停用", HttpStatus.FORBIDDEN),
    BAD_CREDENTIALS(40027, "账号或密码错误", HttpStatus.BAD_REQUEST),
    CONFLICT(40029, "数据已被其他操作修改，请刷新后重试", HttpStatus.CONFLICT),

    INTERNAL_ERROR(50001, "服务器开小差了，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
