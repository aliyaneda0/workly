package com.aliya.workly.company;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyServiceImpl companyService;

    @Test
    void findAll_mapsEveryCompanyToDTO() {
        Company company = new Company();
        company.setId(1L);
        company.setCompanyName("Acme Corp");

        when(companyRepository.findAll()).thenReturn(List.of(company));

        List<CompanyDTO> result = companyService.findAll();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("Acme Corp");
    }

    @Test
    void findById_whenExists_returnsDTO() {
        Company company = new Company();
        company.setId(1L);
        company.setCompanyName("Acme Corp");

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        CompanyDTO result = companyService.findById(1L);

        assertThat(result.getCompanyName()).isEqualTo("Acme Corp");
    }

    @Test
    void findById_whenMissing_returnsNull() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(companyService.findById(99L));
    }

    @Test
    void save_savesAndReturnsDTO() {
        Company saved = new Company();
        saved.setId(1L);
        saved.setCompanyName("Acme Corp");

        when(companyRepository.save(any(Company.class))).thenReturn(saved);

        CompanyDTO input = new CompanyDTO(null, "Acme Corp", "A company", "Remote");
        CompanyDTO result = companyService.save(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCompanyName()).isEqualTo("Acme Corp");
        verify(companyRepository, times(1)).save(any(Company.class));
    }

    @Test
    void update_whenExists_overwritesFieldsAndSaves() {
        Company existing = new Company();
        existing.setId(1L);
        existing.setCompanyName("Old Name");

        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyDTO input = new CompanyDTO(null, "New Name", "New description", "New location");
        CompanyDTO result = companyService.update(1L, input);

        assertThat(result.getCompanyName()).isEqualTo("New Name");
        assertThat(result.getLocation()).isEqualTo("New location");
    }

    @Test
    void update_whenMissing_returnsNullWithoutSaving() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        CompanyDTO input = new CompanyDTO(null, "New Name", "New description", "New location");
        assertNull(companyService.update(99L, input));

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void deleteById_whenExists_deletesAndReturnsTrue() {
        when(companyRepository.existsById(1L)).thenReturn(true);

        assertThat(companyService.deleteById(1L)).isTrue();

        verify(companyRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteById_whenMissing_returnsFalseWithoutDeleting() {
        when(companyRepository.existsById(99L)).thenReturn(false);

        assertThat(companyService.deleteById(99L)).isFalse();

        verify(companyRepository, never()).deleteById(any(Long.class));
    }
}
