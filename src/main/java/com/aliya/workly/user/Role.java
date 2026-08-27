package com.aliya.workly.user;

public enum Role {
    APPLICANT, // browse jobs/companies/reviews, write reviews
    COMPANY,   // + create/update/delete jobs for their own company
    ADMIN      // any company, any job, any review
}
