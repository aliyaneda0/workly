package com.aliya.fetchjobapp.review;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/company/{companyId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<ReviewDTO>> getAllReviews(@PathVariable Long companyId) {
        return ResponseEntity.ok(reviewService.findAllByCompanyId(companyId));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long companyId,
                                                   @PathVariable Long reviewId) {
        ReviewDTO review = reviewService.findById(companyId, reviewId);
        if (review == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(review);
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@PathVariable Long companyId,
                                                  @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO created = reviewService.save(companyId, reviewDTO);
        if (created == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReview(@PathVariable Long companyId,
                                                  @PathVariable Long reviewId,
                                                  @RequestBody ReviewDTO reviewDTO) {
        ReviewDTO updated = reviewService.update(companyId, reviewId, reviewDTO);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long companyId,
                                             @PathVariable Long reviewId) {
        boolean deleted = reviewService.deleteById(companyId, reviewId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }


}
