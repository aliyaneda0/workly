package com.aliya.workly.review;

import com.aliya.workly.company.Company;
import com.aliya.workly.company.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Company companyWithId(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    private Review reviewOwnedBy(Long reviewId, Long companyId, Long ownerId) {
        Review review = new Review();
        review.setId(reviewId);
        review.setCompany(companyWithId(companyId));
        review.setReviewedBy(ownerId);
        review.setTitle("Original title");
        return review;
    }

    @Test
    void findAllByCompanyId_mapsEveryReviewToDTO() {
        Review review = reviewOwnedBy(1L, 5L, 42L);
        when(reviewRepository.findByCompanyId(5L)).thenReturn(List.of(review));

        List<ReviewDTO> result = reviewService.findAllByCompanyId(5L);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Original title");
    }

    @Test
    void findById_whenCompanyMatches_returnsDTO() {
        Review review = reviewOwnedBy(1L, 5L, 42L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ReviewDTO result = reviewService.findById(5L, 1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenCompanyMismatch_returnsNull() {
        Review review = reviewOwnedBy(1L, 5L, 42L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        // review belongs to company 5, asking for it under company 999 — must not leak across companies
        assertNull(reviewService.findById(999L, 1L));
    }

    @Test
    void save_whenCompanyExists_setsReviewedByFromCallerNotDTO() {
        when(companyRepository.findById(5L)).thenReturn(Optional.of(companyWithId(5L)));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDTO input = new ReviewDTO(null, "Great place", "Loved it", 4.5, 5L);
        input.setReviewedBy(999L); // attacker-controlled value in the request body — must be ignored

        ReviewDTO result = reviewService.save(5L, input, 42L); // 42L is the authenticated caller

        assertThat(result.getReviewedBy()).isEqualTo(42L);
    }

    @Test
    void save_whenCompanyMissing_returnsNull() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        ReviewDTO input = new ReviewDTO(null, "Great place", "Loved it", 4.5, 99L);
        assertNull(reviewService.save(99L, input, 42L));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void update_whenCallerIsOwner_updatesFields() {
        Review review = reviewOwnedBy(1L, 5L, 42L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDTO input = new ReviewDTO(null, "Updated title", "Updated body", 3.0, 5L);
        ReviewDTO result = reviewService.update(5L, 1L, input, 42L, false);

        assertThat(result.getTitle()).isEqualTo("Updated title");
    }

    @Test
    void update_whenCallerIsAdminButNotOwner_updatesFields() {
        Review review = reviewOwnedBy(1L, 5L, 42L); // owned by 42, admin is a different user
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDTO input = new ReviewDTO(null, "Moderated title", "Moderated body", 1.0, 5L);
        ReviewDTO result = reviewService.update(5L, 1L, input, 777L, true);

        assertThat(result.getTitle()).isEqualTo("Moderated title");
    }

    @Test
    void update_whenCallerIsNeitherOwnerNorAdmin_throwsAccessDenied() {
        Review review = reviewOwnedBy(1L, 5L, 42L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ReviewDTO input = new ReviewDTO(null, "Hijacked title", "Hijacked body", 1.0, 5L);

        assertThrows(AccessDeniedException.class,
                () -> reviewService.update(5L, 1L, input, 777L, false));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void deleteById_whenCallerIsOwner_deletesAndReturnsTrue() {
        Review review = reviewOwnedBy(1L, 5L, 42L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThat(reviewService.deleteById(5L, 1L, 42L, false)).isTrue();

        verify(reviewRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteById_whenCallerIsNeitherOwnerNorAdmin_throwsAccessDenied() {
        Review review = reviewOwnedBy(1L, 5L, 42L);
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        assertThrows(AccessDeniedException.class,
                () -> reviewService.deleteById(5L, 1L, 777L, false));

        verify(reviewRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteById_whenMissing_returnsFalse() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(reviewService.deleteById(5L, 99L, 42L, false)).isFalse();
    }
}
