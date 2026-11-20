package com.aliya.reviewservice.review;

import com.aliya.reviewservice.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {
    PageResponse<ReviewDTO> findAllByCompanyId(Long companyId, Pageable pageable);

    ReviewDTO findById(Long companyId, Long reviewId);

    // acting user comes from the token, not the DTO (see security checklist)
    ReviewDTO save(Long companyId, ReviewDTO reviewDTO, Long reviewedByUserId);

    // actingUserId/isAdmin enforce "own review or admin" — see the IDOR entry
    // in the security checklist. The DTO's own reviewedBy field is never trusted for this.
    ReviewDTO update(Long companyId, Long reviewId, ReviewDTO reviewDTO, Long actingUserId, boolean isAdmin);

    boolean deleteById(Long companyId, Long reviewId, Long actingUserId, boolean isAdmin);
}
