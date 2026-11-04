package com.aliya.workly.user;

import jakarta.validation.constraints.NotNull;
import org.aspectj.bridge.IMessage;

public class CompanyAssignmentDto {

    @NotNull(message = "companyId is required")
    private Long companyId;

    public Long getCompanyId(){
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

}