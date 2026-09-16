package com.aliya.workly.security;

import com.aliya.workly.user.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// The piece that finally lets controllers stop trusting a client-supplied "postedBy"/
// "reviewedBy" field and fill it from the authenticated caller instead — see the
// mass-assignment discussion tied to JobDTO.postedBy earlier, and Section 13 of the blueprint.
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
    // authenticated callers differently (e.g. GET /jobs scoping DRAFT visibility).
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
}
