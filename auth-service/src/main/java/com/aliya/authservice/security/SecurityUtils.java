package com.aliya.authservice.security;

import com.aliya.authservice.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthPrincipal currentPrincipal() {
        AuthPrincipal principal = currentPrincipalOrNull();
        if (principal == null) {
            throw new IllegalStateException("No authenticated user in the current request");
        }
        return principal;
    }

    // Null, not a throw — for endpoints that permitAll and serve both anonymous and
    // authenticated callers differently.
    public static AuthPrincipal currentPrincipalOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            return null;
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentPrincipal().getId();
    }

    public static Role currentRole() {
        return currentPrincipal().getRole();
    }

    public static boolean isAdmin() {
        return currentRole() == Role.ADMIN;
    }

    public static Long currentCompanyId() {
        return currentPrincipal().getCompanyId();
    }
}
