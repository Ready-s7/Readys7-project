package com.example.readys7project.domain.review.service;

import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequestDto;
import com.example.readys7project.domain.review.enums.ReviewRole;
import com.example.readys7project.domain.user.client.service.ClientService;
import com.example.readys7project.domain.user.developer.service.DeveloperService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewTransactionService {

    private final ReviewService reviewService;
    private final DeveloperService developerService;
    private final ClientService clientService;

    // @Transactional 없음 - 퍼사드 역할
    public ReviewDto createReviewWithRatingUpdate(
            ReviewRequestDto request, Long targetUserId, String email) {

        // 1. ReviewService의 @Transactional createReview 커밋 완료
        ReviewDto result = reviewService.createReview(request, targetUserId, email);

        // 2. 커밋 완료 후 평점 갱신
        if (result.writerRole() == ReviewRole.CLIENT) {
            developerService.updateRating(result.developerId());
        } else if (result.writerRole() == ReviewRole.DEVELOPER) {
            clientService.updateRating(result.clientId());
        }

        return result;
    }
}
