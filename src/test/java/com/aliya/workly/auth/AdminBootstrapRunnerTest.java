package com.aliya.workly.auth;

import com.aliya.workly.user.AuthProvider;
import com.aliya.workly.user.Role;
import com.aliya.workly.user.User;
import com.aliya.workly.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminBootstrapRunner runnerWith(String email, String password) {
        return new AdminBootstrapRunner(userRepository, passwordEncoder, email, password);
    }

    @Test
    void run_emailAndPasswordSet_createsLocalAdminWithHashedPassword() {
        when(userRepository.existsByEmail("admin@workly.com")).thenReturn(false);
        when(passwordEncoder.encode("s3cret-pass")).thenReturn("hashed");

        runnerWith("  Admin@Workly.com ", "s3cret-pass").run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("admin@workly.com");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void run_emailAlreadyRegistered_leavesTheAccountAlone() {
        when(userRepository.existsByEmail("admin@workly.com")).thenReturn(true);

        runnerWith("admin@workly.com", "s3cret-pass").run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void run_missingEmail_doesNothing() {
        runnerWith("", "s3cret-pass").run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void run_missingPassword_doesNothing() {
        runnerWith("admin@workly.com", "").run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void run_passwordTooShort_failsStartupWithoutCreatingAnything() {
        assertThatThrownBy(() -> runnerWith("admin@workly.com", "short").run(null))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(userRepository);
    }
}
