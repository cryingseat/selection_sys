package com.labselection.auth;

import com.labselection.common.BusinessException;
import com.labselection.common.ErrorCode;
import com.labselection.domain.enums.Role;

public final class AuthContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static CurrentUser getOrNull() {
        return HOLDER.get();
    }

    public static Long userId() {
        return get().userId();
    }

    public static Role role() {
        return get().role();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
