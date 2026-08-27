package com.aliya.workly.review;

import java.util.List;

public interface ReviewService {
    List<ReviewDTO> findAllByCompanyId(Long companyId);

    ReviewDTO findById(Long companyId, Long reviewId);

    // CHANGED: acting user comes from the token, not the DTO (see security checklist)
    ReviewDTO save(Long companyId, ReviewDTO reviewDTO, Long reviewedByUserId);

    // CHANGED: actingUserId/isAdmin enforce "own review or admin" — see the IDOR entry
    // in the security checklist. The DTO's own reviewedBy field is never trusted for this.
    ReviewDTO update(Long companyId, Long reviewId, ReviewDTO reviewDTO, Long actingUserId, boolean isAdmin);

    boolean deleteById(Long companyId, Long reviewId, Long actingUserId, boolean isAdmin);
}
