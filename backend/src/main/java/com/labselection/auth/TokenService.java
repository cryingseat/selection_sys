package com.labselection.auth;

import com.labselection.domain.entity.AuthToken;
import com.labselection.domain.entity.SysUser;
import com.labselection.mapper.TokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenMapper tokenMapper;

    @Value("${app.token-ttl-hours:12}")
    private long ttlHours;

    @Transactional
    public String createToken(SysUser user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AuthToken token = new AuthToken();
        token.setUserId(user.getId());
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(LocalDateTime.now().plusHours(ttlHours));
        token.setRevoked(false);
        tokenMapper.insert(token);
        return raw;
    }

    public CurrentUser authenticate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        AuthToken token = tokenMapper.findValid(sha256(rawToken), LocalDateTime.now());
        if (token == null) {
            return null;
        }
        tokenMapper.touch(token.getId());
        return new CurrentUser(token.getUserId(), token.getAccount(), token.getDisplayName(), token.getRole());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            tokenMapper.revoke(sha256(rawToken));
        }
    }

    @Transactional
    public void cleanExpired() {
        tokenMapper.deleteExpired(LocalDateTime.now());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("摘要算法不可用", ex);
        }
    }
}
