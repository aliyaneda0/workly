package com.aliya.workly.job;


import com.aliya.workly.common.PageResponse;
import com.aliya.workly.company.Company;
import com.aliya.workly.company.CompanyRepository;
import com.aliya.workly.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;

    // constructor injection — never use @Autowired on a field
    public JobServiceImpl(JobRepository jobRepository,
                          CompanyRepository companyRepository) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
    }

    private void requiredCompanyAccess(Long jobCompanyId,Long callerCompanyId,boolean isAdmin){
        if(isAdmin) return ;
        if(callerCompanyId == null || !callerCompanyId.equals(jobCompanyId)){
             throw new AccessDeniedException("Not your company");
        }
    }

    @Override
    @Transactional
    // CORRECT — fetches company, throws 404 if missing
    // Enforced : Validates the creator belongs to the company they are trying to post for
    public JobDTO createJob(JobDTO jobDTO, Long postedByUserId, Long callerCompanyId , boolean isAdmin) {

        // Rule : check against request companyId before creating
        requiredCompanyAccess(jobDTO.getCompanyId(), callerCompanyId, isAdmin);


        Company company = companyRepository.findById(jobDTO.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + jobDTO.getCompanyId()));
        Job job = toEntity(jobDTO, company);  // ← toEntity takes dto + company
        job.setPostedBy(postedByUserId); // CHANGED: from the authenticated caller, never from jobDTO
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

     // ENFORCED : Loads the database row first, checks ownership, restricts company reassignment to ADMINs
    public JobDTO updateJob(Long id, JobDTO jobDTO, Long callerCompanyId ,boolean isAdmin) {

         // RULE : Load the job first from database to check actual secure state

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with id: " + id));

        // Rule : Check if caller has access to the existing job's company
        requiredCompanyAccess(job.getCompany().getId(),callerCompanyId , isAdmin);

        job.setTitle(jobDTO.getTitle());
        job.setDescription(jobDTO.getDescription());
        job.setMinSalary(jobDTO.getMinSalary());
        job.setMaxSalary(jobDTO.getMaxSalary());
        job.setLocation(jobDTO.getLocation());
        if (jobDTO.getStatus() != null) job.setStatus(jobDTO.getStatus());

        // if companyId changed, re-fetch and update
        // Rule : Only ADMIN may change companyId if it is different from the stored row

        if (jobDTO.getCompanyId() != null && !jobDTO.getCompanyId().equals(job.getCompany().getId())) {

            if(!isAdmin){
                throw new AccessDeniedException("Only admins can reassign a job to a different company");
            }

            Company company = companyRepository.findById(jobDTO.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Company not found with id: " + jobDTO.getCompanyId()));
            job.setCompany(company);
        }

        return toDTO(job);
    }

    @Override
    @Transactional

    //ENFORCED : Loads the job row first , validates ownership,and only deletes if authorized
    public boolean deleteById(Long id , Long callerCompanyId , boolean isAdmin) {

        // Rule 4 : Load the job first, then check company access

        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found with id: " + id);
        }

        Optional<Job> job = jobRepository.findById(id);

        // Check against the secure database row's company id, never request input
        requiredCompanyAccess(job.get().getCompany().getId(), callerCompanyId, isAdmin);

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
        dto.setCompanyId(job.getCompany().getId());
        dto.setPostedBy(job.getPostedBy());
         return dto;
    }

    private Job toEntity(JobDTO dto, Company company) {
        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setMinSalary(dto.getMinSalary());
        job.setMaxSalary(dto.getMaxSalary());
        job.setLocation(dto.getLocation());
        job.setStatus(dto.getStatus() != null ? dto.getStatus() : JobStatus.DRAFT);
        job.setCompany(company);
        return job;
    }


}
