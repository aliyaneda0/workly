package com.aliya.workly.user;

// Outbound only — deliberately has no passwordHash field, not just a hidden one.
// See docs/AUTH_GUIDE.md and the Workly Auth Blueprint (Section 5) for why this is split
// from RegisterRequest/LoginRequest instead of reusing one DTO both directions.
public class UserResponse {

    private final Long id;
    private final String email;
    private final String fullName;
    private final Role role;

    public UserResponse(Long id, String email, String fullName, Role role) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }
}
