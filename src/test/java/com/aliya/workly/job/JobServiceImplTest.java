package com.aliya.workly.job;

import com.aliya.workly.company.Company;
import com.aliya.workly.company.CompanyRepository;
import com.aliya.workly.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;      // ✅ Mockito, not Hamcrest
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void getJobById_whenJobExists_returnsJobDTO() {
        // Build a Company AND attach it to the Job
        Company company = new Company();
        company.setId(1L);

        Job job = new Job();
        job.setId(1L);
        job.setCompany(company); // ← was missing, caused Error 2

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        JobDTO result = jobService.getJobById(1L);
        assertNotNull(result);
    }

    @Test
    void getJobById_whenJobMissing_throwsResourceNotFoundException() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> jobService.getJobById(99L));
    }
    @Test
    void createJob_savesAndReturnsJobDTO() {

        Company company = new Company();
        company.setId(1L);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        JobDTO input = new JobDTO();
        input.setTitle("Java Developer");
        input.setCompanyId(1L); // ← FIX 1: tell the service which company to look up

        Job savedJob = new Job();
        savedJob.setTitle("Java Developer");
        savedJob.setCompany(company); // ← FIX 2: toDTO() needs this to not crash

        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        JobDTO result = jobService.createJob(input);

        assertThat(result.getTitle()).isEqualTo("Java Developer");
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    // ✅ bogus private verify() method removed

    @Test
    void deleteJob_whenJobMissing_throwsException() {
        when(jobRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> jobService.deleteById(99L));

        verify(jobRepository, never()).deleteById(any(Long.class)); // ✅ typed
    }
}