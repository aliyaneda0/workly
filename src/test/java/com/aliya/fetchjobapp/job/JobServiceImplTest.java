package com.aliya.fetchjobapp.job;

import com.aliya.fetchjobapp.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;      // ✅ Mockito, not Hamcrest
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock
    private JobRepository jobRepository;

    @InjectMocks
    private JobServiceImpl jobService;

    @Test
    void getJobById_whenJobExists_returnsJobDTO() {
        Job job = new Job();
        job.setTitle("Backend Developer");
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        JobDTO result = jobService.getJobById(1L);

        assertThat(result.getTitle()).isEqualTo("Backend Developer");
    }

    @Test
    void getJobById_whenJobMissing_throwsResourceNotFoundException() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> jobService.getJobById(99L));
    }

    @Test
    void createJob_savesAndReturnsJobDTO() {
        JobDTO input = new JobDTO();
        input.setTitle("Java Developer");

        Job savedJob = new Job();
        savedJob.setTitle("Java Developer");
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