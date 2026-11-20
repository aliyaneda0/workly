package com.aliya.reviewservice.review;


import com.aliya.reviewservice.common.PageResponse;
import com.aliya.reviewservice.company.CompanyClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final CompanyClient companyClient;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             CompanyClient companyClient) {
        this.reviewRepository = reviewRepository;
        this.companyClient = companyClient;
    }

    @Override
    public PageResponse<ReviewDTO> findAllByCompanyId(Long companyId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByCompanyId(companyId, pageable);
        return PageResponse.of(reviews.map(this::toDTO));
    }

    @Override
    public ReviewDTO findById(Long companyId, Long reviewId) {
        return reviewRepository.findById(reviewId)
                .filter(r -> r.getCompanyId() != null && r.getCompanyId().equals(companyId))
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public ReviewDTO save(Long companyId, ReviewDTO reviewDTO, Long reviewedByUserId) {
        // Same shape as job-service's CompanyClient check (see DECISIONS.md ADR-010 in the main
        // app) — this used to be companyRepository.existsById(companyId) against a local table;
        // company now lives in its own database, so this asks company-service over HTTP instead.
        if (!companyClient.existsById(companyId)) {
            return null;
        }
        Review review = toEntity(reviewDTO);
        review.setCompanyId(companyId);
        review.setReviewedBy(reviewedByUserId); // from the authenticated caller, never from reviewDTO
        Review saved = reviewRepository.save(review);
        return toDTO(saved);
    }

    @Override
    public ReviewDTO update(Long companyId, Long reviewId, ReviewDTO reviewDTO, Long actingUserId, boolean isAdmin) {
        Optional<Review> optionalReview = reviewRepository.findById(reviewId);
        if (optionalReview.isEmpty()) {
            return null;
        }
        Review review = optionalReview.get();
        if (review.getCompanyId() == null || !review.getCompanyId().equals(companyId)) {
            return null;
        }
        // Ownership check against the EXISTING review's reviewedBy — never trust reviewDTO's own
        // reviewedBy for this, that's the same mass-assignment trap as JobDTO.postedBy. See the
        // IDOR entry in the security checklist.
        requireOwnerOrAdmin(review, actingUserId, isAdmin);

        review.setTitle(reviewDTO.getTitle());
        review.setDescription(reviewDTO.getDescription());
        review.setRating(reviewDTO.getRating());
        Review updated = reviewRepository.save(review);
        return toDTO(updated);
    }

    @Override
    public boolean deleteById(Long companyId, Long reviewId, Long actingUserId, boolean isAdmin) {
        Optional<Review> optionalReview = reviewRepository.findById(reviewId);
        if (optionalReview.isEmpty()) {
            return false;
        }
        Review review = optionalReview.get();
        if (review.getCompanyId() == null || !review.getCompanyId().equals(companyId)) {
            return false;
        }
        requireOwnerOrAdmin(review, actingUserId, isAdmin);

        reviewRepository.deleteById(reviewId);
        return true;
    }

    private void requireOwnerOrAdmin(Review review, Long actingUserId, boolean isAdmin) {
        boolean isOwner = review.getReviewedBy() != null && review.getReviewedBy().equals(actingUserId);
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Only the review's author or an admin can do that");
        }
    }

    private ReviewDTO toDTO(Review review) {
        Long companyId = review.getCompanyId();
        ReviewDTO dto = new ReviewDTO(
                review.getId(),
                review.getTitle(),
                review.getDescription(),
                review.getRating(),
                companyId
        );
        dto.setReviewedBy(review.getReviewedBy());
        return dto;
    }

    private Review toEntity(ReviewDTO dto) {
        Review review = new Review();
        review.setTitle(dto.getTitle());
        review.setDescription(dto.getDescription());
        review.setRating(dto.getRating());
        return review;
    }

}
