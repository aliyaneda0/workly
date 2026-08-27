package com.aliya.workly.auth.dto;

public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn; // access token TTL, seconds — client knows when to refresh
    private final String tokenType = "Bearer";

    public AuthResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }
}
