package com.aliya.authservice.auth;

import com.aliya.authservice.auth.dto.AuthResponse;
import com.aliya.authservice.auth.dto.LoginRequest;
import com.aliya.authservice.auth.dto.RefreshRequest;
import com.aliya.authservice.auth.dto.RegisterRequest;
import com.aliya.authservice.security.SecurityUtils;
import com.aliya.authservice.user.User;
import com.aliya.authservice.user.UserMapper;
import com.aliya.authservice.user.UserRepository;
import com.aliya.authservice.user.UserResponse;
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

    // Convenience endpoint for testing the whole flow with a real token.
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        User user = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }
}
