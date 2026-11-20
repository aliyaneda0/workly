package com.aliya.jobservice.job;

import com.aliya.jobservice.common.PageResponse;
import com.aliya.jobservice.company.CompanyClient;
import com.aliya.jobservice.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyClient companyClient;

    public JobServiceImpl(JobRepository jobRepository,
                           CompanyClient companyClient) {
        this.jobRepository = jobRepository;
        this.companyClient = companyClient;
    }

    private void requiredCompanyAccess(Long jobCompanyId, Long callerCompanyId, boolean isAdmin) {
        if (isAdmin) return;
        if (callerCompanyId == null || !callerCompanyId.equals(jobCompanyId)) {
            throw new AccessDeniedException("Not your company");
        }
    }

    @Override
    @Transactional
    public JobDTO createJob(JobDTO jobDTO, Long postedByUserId, Long callerCompanyId, boolean isAdmin) {

        requiredCompanyAccess(jobDTO.getCompanyId(), callerCompanyId, isAdmin);

        if (!companyClient.existsById(jobDTO.getCompanyId())) {
            throw new ResourceNotFoundException("Company not found with id: " + jobDTO.getCompanyId());
        }
        Job job = toEntity(jobDTO);
        job.setPostedBy(postedByUserId);
        return toDTO(jobRepository.save(job));
    }

    @Override
    public PageResponse<JobDTO> getAllJobs(JobSearchCriteria criteria, Pageable pageable, Long callerId, boolean isAdmin) {
        Page<Job> jobs = jobRepository.findAll(JobSpecifications.fromCriteria(criteria, callerId, isAdmin), pageable);
        return PageResponse.of(jobs.map(this::toDTO));
    }

    @Override
    public JobDTO getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with id: " + id));
        return toDTO(job);
    }

    @Override
    @Transactional
    public JobDTO updateJob(Long id, JobDTO jobDTO, Long callerCompanyId, boolean isAdmin) {

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with id: " + id));

        requiredCompanyAccess(job.getCompanyId(), callerCompanyId, isAdmin);

        job.setTitle(jobDTO.getTitle());
        job.setDescription(jobDTO.getDescription());
        job.setMinSalary(jobDTO.getMinSalary());
        job.setMaxSalary(jobDTO.getMaxSalary());
        job.setLocation(jobDTO.getLocation());
        if (jobDTO.getStatus() != null) job.setStatus(jobDTO.getStatus());

        if (jobDTO.getCompanyId() != null && !jobDTO.getCompanyId().equals(job.getCompanyId())) {

            if (!isAdmin) {
                throw new AccessDeniedException("Only admins can reassign a job to a different company");
            }

            if (!companyClient.existsById(jobDTO.getCompanyId())) {
                throw new ResourceNotFoundException("Company not found with id: " + jobDTO.getCompanyId());
            }
            job.setCompanyId(jobDTO.getCompanyId());
        }

        return toDTO(job);
    }

    @Override
    @Transactional
    public boolean deleteById(Long id, Long callerCompanyId, boolean isAdmin) {

        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found with id: " + id);
        }

        Optional<Job> job = jobRepository.findById(id);

        requiredCompanyAccess(job.get().getCompanyId(), callerCompanyId, isAdmin);

        jobRepository.deleteById(id);
        return true;
    }

    // ── private mappers ──────────────────────────────────────

    private JobDTO toDTO(Job job) {
        JobDTO dto = new JobDTO();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setMinSalary(job.getMinSalary());
        dto.setMaxSalary(job.getMaxSalary());
        dto.setLocation(job.getLocation());
        dto.setStatus(job.getStatus());
        dto.setCompanyId(job.getCompanyId());
        dto.setPostedBy(job.getPostedBy());
        return dto;
    }

    private Job toEntity(JobDTO dto) {
        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setMinSalary(dto.getMinSalary());
        job.setMaxSalary(dto.getMaxSalary());
        job.setLocation(dto.getLocation());
        job.setStatus(dto.getStatus() != null ? dto.getStatus() : JobStatus.DRAFT);
        job.setCompanyId(dto.getCompanyId());
        return job;
    }
}
