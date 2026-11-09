package com.labselection.service;

import com.labselection.auth.AuthContext;
import com.labselection.auth.CurrentUser;
import com.labselection.domain.entity.AuditLog;
import com.labselection.mapper.AuditMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditMapper auditMapper;

    public void log(Long batchId, String action, String targetType, Long targetId,
                    String detail, String reason) {
        CurrentUser actor = AuthContext.getOrNull();
        AuditLog log = new AuditLog();
        log.setBatchId(batchId);
        if (actor != null) {
            log.setActorId(actor.userId());
            log.setActorName(actor.displayName());
            log.setActorRole(actor.role().name());
        }
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setReason(reason);
        log.setIp(clientIp());
        auditMapper.insert(log);
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception ex) {
            return null;
        }
    }
}
