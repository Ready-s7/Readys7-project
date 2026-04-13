package com.example.readys7project.domain.review.repository;

import com.example.readys7project.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewQueryRepository {

    // 클라이언트 조회
    Page<Review> searchReviewsByClient(
            Long clientId,
            Integer rating,
            Integer minRating,
            Integer maxRating,
            Pageable pageable
    );


// 개발자 조회
    Page<Review> searchReviewsByDeveloper(
            Long developerId,
            Integer rating,
            Integer minRating,
            Integer maxRating,
            Pageable pageable
    );
}
