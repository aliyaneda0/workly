package com.aliya.fetchjobapp.review;

import java.util.List;

public interface ReviewService {
    List<ReviewDTO> findAllByCompanyId(Long companyId);

    ReviewDTO findById(Long companyId, Long reviewId);

    ReviewDTO save(Long companyId, ReviewDTO reviewDTO);

    ReviewDTO update(Long companyId, Long reviewId, ReviewDTO reviewDTO);

    boolean deleteById(Long companyId, Long reviewId);
}
