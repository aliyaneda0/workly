package com.aliya.fetchjobapp.company;

public class CompanyDTO {

    private Long id;

    private String companyName;

    private String description;

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
