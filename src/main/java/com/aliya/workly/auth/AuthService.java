package com.aliya.workly.auth;

import com.aliya.workly.auth.dto.AuthResponse;
import com.aliya.workly.auth.dto.LoginRequest;
import com.aliya.workly.auth.dto.RegisterRequest;
import com.aliya.workly.exception.EmailAlreadyExistsException;
import com.aliya.workly.security.JwtService;
import com.aliya.workly.user.AuthProvider;
import com.aliya.workly.user.Role;
import com.aliya.workly.user.User;
import com.aliya.workly.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        // Closes the same mass-assignment gap as JobDTO.postedBy, but for the highest-stakes
        // field in the system — see the security checklist, "Client-controlled role on signup".
        if (req.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot self-register as ADMIN");
        }
        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword())); // never store the raw password
        user.setFullName(req.getFullName());
        user.setRole(req.getRole());
        user.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(user);

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            // Same message for "no such user" and "wrong password" on purpose — see the
            // security checklist entry on user enumeration.
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        Long userId = jwtService.extractUserId(claims);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        // NOTE: this reissues a refresh token but does not yet revoke the old one — there's no
        // server-side token store. That means reuse-detection (the "important" item in the
        // security checklist under JWT & refresh tokens) is NOT implemented yet. Tracked as a
        // follow-up: a RefreshToken entity storing a hash per issued token, checked and rotated
        // here. Fine for local development, not for a real deployment.
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, jwtService.getAccessTtlSeconds());
    }
}
