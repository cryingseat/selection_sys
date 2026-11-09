package com.labselection;

import com.labselection.auth.PasswordService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void shouldEncodeAndVerifyPassword() {
        String hash = passwordService.encode("123456");
        assertTrue(hash.startsWith("pbkdf2$120000$"));
        assertTrue(passwordService.matches("123456", hash));
    }

    @Test
    void shouldRejectWrongPasswordAndMalformedHash() {
        String hash = passwordService.encode("123456");
        assertFalse(passwordService.matches("654321", hash));
        assertFalse(passwordService.matches("123456", "plain-text"));
        assertFalse(passwordService.matches(null, hash));
    }
}
