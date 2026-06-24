package com.aliya.fetchjobapp.job;


import com.aliya.fetchjobapp.company.Company;
import com.aliya.fetchjobapp.company.CompanyRepository;
import com.aliya.fetchjobapp.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    @Transactional
    // CORRECT — fetches company, throws 404 if missing
    public JobDTO createJob(JobDTO jobDTO) {
        Company company = companyRepository.findById(jobDTO.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found with id: " + jobDTO.getCompanyId()));
        Job job = toEntity(jobDTO, company);  // ← toEntity takes dto + company
        return toDTO(jobRepository.save(job));
    }

    @Override
    public List<JobDTO> getAllJobs() {
        return jobRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
    public JobDTO updateJob(Long id, JobDTO jobDTO) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found with id: " + id));

        job.setTitle(jobDTO.getTitle());
        job.setDescription(jobDTO.getDescription());
        job.setMinSalary(jobDTO.getMinSalary());
        job.setMaxSalary(jobDTO.getMaxSalary());
        job.setLocation(jobDTO.getLocation());
        if (jobDTO.getStatus() != null) job.setStatus(jobDTO.getStatus());

        // if companyId changed, re-fetch and update
        if (jobDTO.getCompanyId() != null &&
                !jobDTO.getCompanyId().equals(job.getCompany().getId())) {
            Company company = companyRepository.findById(jobDTO.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Company not found with id: " + jobDTO.getCompanyId()));
            job.setCompany(company);
        }

        return toDTO(job);
    }

    @Override
    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found with id: " + id);
        }
        jobRepository.deleteById(id);
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
        return job;
    }


}
