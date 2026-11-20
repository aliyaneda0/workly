package com.aliya.jobservice.job;

import com.aliya.jobservice.common.PageResponse;
import com.aliya.jobservice.company.CompanyClient;
import com.aliya.jobservice.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    // Replaces the monolith's mocked CompanyRepository — job-service checks company
    // existence over HTTP (CompanyClient) instead of a local repository.
    @Mock
    private CompanyClient companyClient;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void getJobById_whenJobExists_returnsJobDTO() {
        Job job = new Job();
        job.setId(1L);
        job.setCompanyId(1L);

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

        when(companyClient.existsById(1L)).thenReturn(true);

        JobDTO input = new JobDTO();
        input.setTitle("Java Developer");
        input.setCompanyId(1L);

        Job savedJob = new Job();
        savedJob.setTitle("Java Developer");
        savedJob.setCompanyId(1L);

        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

        JobDTO result = jobService.createJob(input, 42L, 1L, false);

        assertThat(result.getTitle()).isEqualTo("Java Developer");
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void createJob_whenCompanyMissing_throwsResourceNotFoundExceptionWithoutSaving() {

        when(companyClient.existsById(99L)).thenReturn(false);

        JobDTO input = new JobDTO();
        input.setTitle("Java Developer");
        input.setCompanyId(99L);

        assertThrows(ResourceNotFoundException.class,
                () -> jobService.createJob(input, 42L, 99L, false));

        verify(jobRepository, never()).save(any(Job.class));
    }

    @Test
    void getAllJobs_mapsPageOfJobsToPageResponseOfDTOs() {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Backend Engineer");
        job.setCompanyId(1L);

        Pageable pageable = PageRequest.of(0, 20);
        when(jobRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(job), pageable, 1));

        JobSearchCriteria criteria = new JobSearchCriteria(null, null, null, null, null);
        PageResponse<JobDTO> result = jobService.getAllJobs(criteria, pageable, null, false);

        assertThat(result.content().size()).isEqualTo(1);
        assertThat(result.content().get(0).getTitle()).isEqualTo("Backend Engineer");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void deleteJob_whenJobMissing_throwsException() {
        when(jobRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> jobService.deleteById(99L, 1L, false));

        verify(jobRepository, never()).deleteById(any(Long.class));
    }
}
