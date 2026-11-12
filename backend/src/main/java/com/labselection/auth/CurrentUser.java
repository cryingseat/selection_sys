package com.labselection.auth;

import com.labselection.domain.enums.Role;

public record CurrentUser(Long userId, String account, String displayName, Role role,
                          Long collegeId, String collegeName) {
}
