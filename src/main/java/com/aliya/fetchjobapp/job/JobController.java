package com.aliya.fetchjobapp.job;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    @PostMapping("/post/job")
    public ResponseEntity<JobDTO> createJob(@Valid @RequestBody JobDTO jobDTO){

         JobDTO created = jobService.createJob(jobDTO);

          return ResponseEntity.status(HttpStatus.CREATED).body(created);

         }

}
