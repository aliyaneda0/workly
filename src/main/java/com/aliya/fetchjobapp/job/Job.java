package com.aliya.fetchjobapp.job;

import com.aliya.fetchjobapp.company.Company;
import jakarta.persistence.*;
import org.hibernate.annotations.ValueGenerationType;

import java.math.BigDecimal;

@Entity
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String location;

    private JobStatus status;

    private BigDecimal minSalary;


    private BigDecimal maxSalary;

    private Long postedBy;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;




    protected Job() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public BigDecimal getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(BigDecimal minSalary) {
        this.minSalary = minSalary;
    }

    public BigDecimal getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(BigDecimal maxSalary) {
        this.maxSalary = maxSalary;
    }


    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Long getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(Long postedBy) {
        this.postedBy = postedBy;
    }


//    public Job(long id, String title, String description, String location, String status, BigDecimal minSalary, BigDecimal maxSalary) {
//        this.id = id;
//        this.title = title;
//        this.description = description;
//        this.status = status;
//        this.location = location;
//        this.minSalary = minSalary;
//        this.maxSalary = maxSalary;
//    }
}
