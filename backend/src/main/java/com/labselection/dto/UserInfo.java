package com.labselection.dto;

import com.labselection.domain.enums.Role;

public record UserInfo(Long id, String account, String displayName, Role role,
                       Long collegeId, String collegeName, Object profile) {
}
