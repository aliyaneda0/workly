package com.aliya.workly.review;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public class ReviewDTO {

    private Long id;

    // CHANGED: added validation (Phase 0 cleanup — see docs/ROADMAP.md)
    @NotBlank(message = "Review title is required")
    private String title;

    private String description;

    // CHANGED: added validation (Phase 0 cleanup — see docs/ROADMAP.md)
    @DecimalMin(value = "0.0", message = "Rating must be at least 0")
    @DecimalMax(value = "5.0", message = "Rating must be at most 5")
    private double rating;

    private Long companyId;

    private Long reviewedBy;

    public ReviewDTO(Long id, String title, String description, double rating, Long companyId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.rating = rating;
        this.companyId = companyId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }

}
