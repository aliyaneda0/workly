package com.aliya.workly.review;


import com.aliya.workly.company.Company;
import com.aliya.workly.company.CompanyRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService{

    private final ReviewRepository reviewRepository;
    private final CompanyRepository companyRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             CompanyRepository companyRepository) {
        this.reviewRepository = reviewRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public List<ReviewDTO> findAllByCompanyId(Long companyId) {
        return reviewRepository.findByCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewDTO findById(Long companyId, Long reviewId) {
        return reviewRepository.findById(reviewId)
                .filter(r -> r.getCompany() != null && r.getCompany().getId().equals(companyId))
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public ReviewDTO save(Long companyId, ReviewDTO reviewDTO, Long reviewedByUserId) {
        Optional<Company> optionalCompany = companyRepository.findById(companyId);
        if (optionalCompany.isEmpty()) {
            return null;
        }
        Review review = toEntity(reviewDTO);
        review.setCompany(optionalCompany.get());
        review.setReviewedBy(reviewedByUserId); // CHANGED: from the authenticated caller, never from reviewDTO
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
        if (review.getCompany() == null || !review.getCompany().getId().equals(companyId)) {
            return null;
        }
        // CHANGED: ownership check against the EXISTING review's reviewedBy — never trust
        // reviewDTO's own reviewedBy for this, that's the same mass-assignment trap as
        // JobDTO.postedBy. See the IDOR entry in the security checklist.
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
        if (review.getCompany() == null || !review.getCompany().getId().equals(companyId)) {
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
        Long companyId = review.getCompany() != null ? review.getCompany().getId() : null;
        ReviewDTO dto = new ReviewDTO(
                review.getId(),
                review.getTitle(),
                review.getDescription(),
                review.getRating(),
                companyId
        );
        dto.setReviewedBy(review.getReviewedBy()); // CHANGED: constructor didn't carry this before
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
