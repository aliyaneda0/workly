package com.aliya.fetchjobapp.job;

import java.util.List;

public interface JobService {

    JobDTO createJob(JobDTO jobDTO);
    List<JobDTO> getAllJobs();
    JobDTO getJobById(Long id);
    JobDTO updateJob(Long id , JobDTO jobDTO);

    boolean deleteById(Long id);
}
