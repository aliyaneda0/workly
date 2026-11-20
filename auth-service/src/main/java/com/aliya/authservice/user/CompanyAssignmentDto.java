package com.aliya.authservice.user;

import jakarta.validation.constraints.NotNull;

public class CompanyAssignmentDto {

    @NotNull(message = "companyId is required")
    private Long companyId;

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

}
