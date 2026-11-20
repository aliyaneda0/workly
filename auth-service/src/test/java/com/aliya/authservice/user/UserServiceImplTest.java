package com.aliya.authservice.user;

import com.aliya.authservice.auth.RefreshTokenService;
import com.aliya.authservice.company.CompanyClient;
import com.aliya.authservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Replaces the monolith's (never-written) mocked CompanyRepository with CompanyClient — same
// swap job-service/review-service already made (see DECISIONS.md ADR-010).
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyClient companyClient;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserServiceImpl userService;

    private User companyUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole(Role.COMPANY);
        return user;
    }

    @Test
    void assignCompanyToUser_whenCompanyExists_assignsAndRevokesExistingTokens() {
        User user = companyUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyClient.existsById(5L)).thenReturn(true);

        userService.assignCompanyToUser(1L, 5L);

        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    void assignCompanyToUser_whenCompanyMissing_throwsWithoutSaving() {
        User user = companyUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyClient.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.assignCompanyToUser(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void assignCompanyToUser_whenUserIsNotCompanyRole_throwsWithoutCallingCompanyClient() {
        User applicant = new User();
        applicant.setId(2L);
        applicant.setRole(Role.APPLICANT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(applicant));

        assertThatThrownBy(() -> userService.assignCompanyToUser(2L, 5L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(companyClient, never()).existsById(any());
    }

    @Test
    void assignCompanyToUser_whenUserMissing_throws() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.assignCompanyToUser(404L, 5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
