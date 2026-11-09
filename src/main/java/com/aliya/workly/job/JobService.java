package com.aliya.workly.job;

import com.aliya.workly.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface JobService {

    JobDTO createJob(JobDTO jobDTO, Long postedByUserId,Long callerCompanyId , boolean isAdmin); // CHANGED: acting user comes from the token, not the DTO

    PageResponse<JobDTO> getAllJobs(JobSearchCriteria criteria, Pageable pageable, Long callerId, boolean isAdmin);

    JobDTO getJobById(Long id);


    JobDTO updateJob(Long id , JobDTO jobDTO , Long callerCompanyId , boolean isAdmin);

    boolean deleteById(Long id , Long callerCompanyId, boolean isAdmin);
}
