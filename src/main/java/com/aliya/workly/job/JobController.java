package com.aliya.workly.job;

import com.aliya.workly.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobController {


    private final JobService jobService;

    public JobController(JobService jobService){

        this.jobService = jobService;
    }
    @GetMapping
    public List<JobDTO> getAllJobs(){

        return jobService.getAllJobs();
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
