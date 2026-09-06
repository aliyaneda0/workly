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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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
        // rotate() does all the validation (exists? used? revoked? expired?) and replay handling,
        // and hands back a fresh refresh token in the same family.
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);

        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(accessToken, rotation.newRefreshToken(), jwtService.getAccessTtlSeconds());
    }

    // Logout = give up the refresh token you hold; we revoke its whole family so no descendant
    // of that login works anymore. The access token still lives until it expires (minutes) —
    // that's the accepted trade-off of stateless access tokens.
    public void logout(String refreshToken) {
        refreshTokenService.revokeFamilyOf(refreshToken);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issueForNewLogin(user.getId());
        return new AuthResponse(accessToken, refreshToken, jwtService.getAccessTtlSeconds());
    }
}
