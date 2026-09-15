package com.aliya.workly.job;

import java.math.BigDecimal;

public record JobSearchCriteria(
        String location,
        JobStatus status,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        String keyword
) {
}
