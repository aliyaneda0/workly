package com.aliya.workly.user;


import com.aliya.workly.auth.RefreshToken;
import com.aliya.workly.auth.RefreshTokenService;
import com.aliya.workly.company.Company;
import com.aliya.workly.company.CompanyRepository;
import com.aliya.workly.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenService refreshTokenService;


    @Override
    @Transactional
    public void assignCompanyToUser(Long userId , Long companyId){

        User user  = userRepository.findById(userId)
                .orElseThrow(()  -> new ResourceNotFoundException("User not found"));

        if(user.getRole() != Role.COMPANY){
            throw new IllegalArgumentException("Target user must have the COMPANY role");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown company"));

        user.setManagedCompany(company);
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());
    }
}
