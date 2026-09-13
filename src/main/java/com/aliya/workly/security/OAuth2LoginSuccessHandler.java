package com.aliya.workly.security;

import com.aliya.workly.auth.AuthService;
import com.aliya.workly.auth.dto.AuthResponse;
import com.aliya.workly.user.AuthProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public OAuth2LoginSuccessHandler(AuthService authService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        AuthProvider provider = switch (token.getAuthorizedClientRegistrationId()) {
            case "google" -> AuthProvider.GOOGLE;
            case "github" -> AuthProvider.GITHUB;
            default -> throw new IllegalStateException(
                    "Unsupported provider: " + token.getAuthorizedClientRegistrationId());
        };

        OAuth2User principal = token.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");

        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        if (email == null || email.isBlank()) {
            write(response, HttpServletResponse.SC_BAD_REQUEST,
                    Map.of("error", "No email available from the " + provider + " account"));
            return;
        }

        AuthResponse tokens = authService.loginWithOAuth(provider, email, name);
        write(response, HttpServletResponse.SC_OK, tokens);
    }

    private void write(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
