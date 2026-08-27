package com.aliya.workly.security;

import com.aliya.workly.user.Role;

// What JwtAuthenticationFilter puts in SecurityContext as Authentication#getPrincipal().
// Carries the caller's id + role straight from the token's claims — no DB lookup per
// request, which is the whole point of stateless JWT auth (see Workly Auth Blueprint, Section 9).
public class AuthPrincipal {

    private final Long id;
    private final String email;
    private final Role role;

    public AuthPrincipal(Long id, String email, Role role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }
}
