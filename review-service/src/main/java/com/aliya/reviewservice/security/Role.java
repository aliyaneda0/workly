package com.aliya.reviewservice.security;

// Mirrors com.aliya.workly.user.Role in the monolith — same JWT "role" claim, decoded here too.
public enum Role {
    APPLICANT,
    COMPANY,
    ADMIN
}
