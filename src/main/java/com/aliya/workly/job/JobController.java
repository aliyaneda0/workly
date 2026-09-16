package com.aliya.workly.job;

import com.aliya.workly.common.PageResponse;
import com.aliya.workly.security.AuthPrincipal;
import com.aliya.workly.security.SecurityUtils;
import com.aliya.workly.user.Role;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/jobs")
public class JobController {


    private final JobService jobService;

    public JobController(JobService jobService){

        this.jobService = jobService;
    }
    @GetMapping
    public PageResponse<JobDTO> getAllJobs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "id") Pageable pageable){

        JobSearchCriteria criteria = new JobSearchCriteria(location, status, minSalary, maxSalary, keyword);
        // GET /jobs is permitAll — an anonymous caller has no AuthPrincipal, so this can't use
        // SecurityUtils.currentUserId()/isAdmin() (those throw when unauthenticated).
        AuthPrincipal principal = SecurityUtils.currentPrincipalOrNull();
        Long callerId = principal != null ? principal.getId() : null;
        boolean isAdmin = principal != null && principal.getRole() == Role.ADMIN;
        return jobService.getAllJobs(criteria, pageable, callerId, isAdmin);
    }

    @GetMapping("/{id}")
    public JobDTO getJobById(@PathVariable Long id){

        return jobService.getJobById(id);
    }
    @PostMapping
    // role check (COMPANY/ADMIN) enforced in SecurityConfig at the filter-chain level
    public ResponseEntity<JobDTO> createJob(@Valid @RequestBody JobDTO jobDTO){

         // CHANGED: postedBy comes from the authenticated caller, not jobDTO — see security checklist
         JobDTO created = jobService.createJob(jobDTO, SecurityUtils.currentUserId());

          return ResponseEntity.status(HttpStatus.CREATED).body(created);

         }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {

        boolean deleted = jobService.deleteById(id);

        if (!deleted) {

            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

}
