package com.aliya.workly.job;

import com.aliya.workly.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface JobService {

    JobDTO createJob(JobDTO jobDTO, Long postedByUserId); // CHANGED: acting user comes from the token, not the DTO
    PageResponse<JobDTO> getAllJobs(JobSearchCriteria criteria, Pageable pageable);
    JobDTO getJobById(Long id);
    JobDTO updateJob(Long id , JobDTO jobDTO);

    boolean deleteById(Long id);
}
