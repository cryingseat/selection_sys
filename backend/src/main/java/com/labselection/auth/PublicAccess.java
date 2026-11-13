package com.labselection.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记无需登录即可访问的接口。
 * 如果请求携带了有效 token，仍会解析出当前用户，用于按学院过滤公开数据。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicAccess {
}
