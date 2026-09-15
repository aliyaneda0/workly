package com.aliya.workly.job;

import org.springframework.data.jpa.domain.Specification;

public final class JobSpecifications {

    private JobSpecifications() {}

    public static Specification<Job> fromCriteria(JobSearchCriteria criteria) {
        return Specification.allOf(
                locationContains(criteria.location()),
                hasStatus(criteria.status()),
                // overlap, not containment: a job matches if its own range reaches into the
                // requested [minSalary, maxSalary] window at all, not only if it sits fully inside it
                maxSalaryAtLeast(criteria.minSalary()),
                minSalaryAtMost(criteria.maxSalary()),
                keywordInTitleOrDescription(criteria.keyword())
        );
    }

    private static Specification<Job> locationContains(String location) {
        if (location == null || location.isBlank()) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%");
    }

    private static Specification<Job> hasStatus(JobStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Job> maxSalaryAtLeast(java.math.BigDecimal minSalary) {
        if (minSalary == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("maxSalary"), minSalary);
    }

    private static Specification<Job> minSalaryAtMost(java.math.BigDecimal maxSalary) {
        if (maxSalary == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("minSalary"), maxSalary);
    }

    private static Specification<Job> keywordInTitleOrDescription(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
