package com.aliya.authservice.user;


import com.aliya.authservice.auth.RefreshTokenService;
import com.aliya.authservice.company.CompanyClient;
import com.aliya.authservice.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CompanyClient companyClient;
    private final RefreshTokenService refreshTokenService;


    @Override
    @Transactional
    public void assignCompanyToUser(Long userId, Long companyId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.COMPANY) {
            throw new IllegalArgumentException("Target user must have the COMPANY role");
        }

        // Same shape as job-service's/review-service's CompanyClient check (see DECISIONS.md
        // ADR-010) — this used to be companyRepository.existsById(companyId) against a local
        // table; company now lives in its own database, so this asks company-service over HTTP.
        if (!companyClient.existsById(companyId)) {
            throw new ResourceNotFoundException("Unknown company");
        }

        user.setManagedCompanyId(companyId);
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());
    }
}
