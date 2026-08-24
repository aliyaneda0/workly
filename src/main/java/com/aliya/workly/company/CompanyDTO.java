package com.aliya.workly.company;

import jakarta.validation.constraints.NotBlank;

public class CompanyDTO {

    private Long id;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String description;

    @NotBlank(message = "Location is required") // CHANGED: added validation (Phase 0 cleanup)
    private String Location;


    public CompanyDTO(Long id, String companyName, String description, String location) {
        this.id = id;
        this.companyName = companyName;
        this.description = description;
        Location = location;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return Location;
    }

    public void setLocation(String location) {
        Location = location;
    }
}
