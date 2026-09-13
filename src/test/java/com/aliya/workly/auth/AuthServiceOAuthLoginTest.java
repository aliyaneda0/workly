package com.aliya.workly.auth;

import com.aliya.workly.auth.dto.AuthResponse;
import com.aliya.workly.security.JwtService;
import com.aliya.workly.user.AuthProvider;
import com.aliya.workly.user.Role;
import com.aliya.workly.user.User;
import com.aliya.workly.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceOAuthLoginTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginWithOAuth_newEmail_createsApplicantForThatProvider() {
        when(userRepository.findByEmail("neda@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(refreshTokenService.issueForNewLogin(any())).thenReturn("refresh");

        AuthResponse result = authService.loginWithOAuth(AuthProvider.GOOGLE, "  Neda@Gmail.com ", "Aliya Neda");

        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(created.capture());
        assertThat(created.getValue().getEmail()).isEqualTo("neda@gmail.com");
        assertThat(created.getValue().getRole()).isEqualTo(Role.APPLICANT);
        assertThat(created.getValue().getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(created.getValue().getPasswordHash()).isNull();
        assertThat(result.getAccessToken()).isEqualTo("access");
        assertThat(result.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    void loginWithOAuth_existingEmail_reusesTheAccount_doesNotCreateAnother() {
        User existing = new User();
        existing.setId(5L);
        existing.setEmail("neda@gmail.com");
        existing.setRole(Role.COMPANY);
        existing.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.findByEmail("neda@gmail.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateAccessToken(existing)).thenReturn("access");
        when(refreshTokenService.issueForNewLogin(5L)).thenReturn("refresh");

        AuthResponse result = authService.loginWithOAuth(AuthProvider.GITHUB, "neda@gmail.com", "whatever");

        verify(userRepository, never()).save(any());
        assertThat(existing.getRole()).isEqualTo(Role.COMPANY);
        assertThat(existing.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(result.getAccessToken()).isEqualTo("access");
        assertThat(result.getRefreshToken()).isEqualTo("refresh");
    }
}
