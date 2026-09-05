package com.aliya.workly.auth;

import com.aliya.workly.auth.dto.AuthResponse;
import com.aliya.workly.auth.dto.LoginRequest;
import com.aliya.workly.auth.dto.RefreshRequest;
import com.aliya.workly.auth.dto.RegisterRequest;
import com.aliya.workly.security.SecurityUtils;
import com.aliya.workly.user.User;
import com.aliya.workly.user.UserMapper;
import com.aliya.workly.user.UserRepository;
import com.aliya.workly.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    // Revokes the refresh-token family this token belongs to. 204 even if the token is already
    // unknown/dead — logout is idempotent, and telling a caller "that token wasn't valid" leaks
    // nothing useful anyway.
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // Convenience endpoint for testing the whole flow with a real token — see Blueprint Section 12.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        User user = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }
}
