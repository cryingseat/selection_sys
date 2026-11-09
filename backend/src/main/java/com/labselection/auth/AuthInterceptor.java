package com.labselection.auth;

import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        String rawToken = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7).trim() : null;

        CurrentUser currentUser = tokenService.authenticate(rawToken);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole != null
                && Arrays.stream(requireRole.value()).noneMatch(role -> role == currentUser.role())) {
            // 角色校验失败时不写入 ThreadLocal，避免线程复用造成身份残留
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        AuthContext.set(currentUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        AuthContext.clear();
    }
}
