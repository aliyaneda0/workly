package com.aliya.jobservice.security;

// What JwtAuthenticationFilter puts in SecurityContext as Authentication#getPrincipal().
// Decoded straight from the token's claims — this service never looks the caller up in a
// database, it trusts what the monolith's auth already signed.
public class AuthPrincipal {

    private final Long id;
    private final String email;
    private final Role role;
    private final Long companyId;

    public AuthPrincipal(Long id, String email, Role role, Long companyId) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.companyId = companyId;
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

    public Long getCompanyId() {
        return companyId;
    }
}
