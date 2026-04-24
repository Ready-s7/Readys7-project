package com.example.readys7project.domain.review.controller;

import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequestDto;
import com.example.readys7project.domain.review.dto.request.ReviewUpdateRequestDto;
import com.example.readys7project.domain.review.service.ReviewService;
import com.example.readys7project.domain.review.service.ReviewTransactionService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import com.example.readys7project.global.aop.CheckOwnerOrAdmin;
import com.example.readys7project.global.aop.EntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewTransactionService reviewTransactionService;
    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping("/v1/reviews")
    public ResponseEntity<ApiResponseDto<ReviewDto>> createReview(
            @Valid @RequestBody ReviewRequestDto request,
            @RequestParam Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        ReviewDto result = reviewTransactionService.createReviewWithRatingUpdate(
                request, targetUserId, email);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, result));
    }

    // 개발자 조회
    @GetMapping(value = "/v1/reviews", params = "developerId")
    public ResponseEntity<ApiResponseDto<Page<ReviewDto>>> getReviewsByDeveloper(
            @RequestParam Long developerId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.getReviewsByDeveloper(developerId, rating, minRating, maxRating, page, size, email)));
    }


    // 클라이언트 조회
    @GetMapping(value = "/v1/reviews", params = "clientId")
    public ResponseEntity<ApiResponseDto<Page<ReviewDto>>> getReviewsByClient(
            @RequestParam Long clientId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    )
    {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.getReviewsByClient(clientId, rating, minRating, maxRating, page, size, email)));
    }

    // 리뷰 수정 (본인 작성 리뷰만 가능 - AOP 검증)
    @PatchMapping("/v1/reviews/{reviewId}")
    @CheckOwnerOrAdmin(type = EntityType.REVIEW, idParam = "reviewId")
    public ResponseEntity<ApiResponseDto<ReviewDto>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.updateReview(reviewId, request, customUserDetails.getEmail())));
    }


    // 리뷰 삭제 (본인 작성 리뷰 / 관리자 가능 - AOP 검증)
    @DeleteMapping("/v1/reviews/{reviewId}")
    @CheckOwnerOrAdmin(type = EntityType.REVIEW, idParam = "reviewId")
    public ResponseEntity<ApiResponseDto<ReviewDto>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        reviewService.deleteReview(reviewId, customUserDetails.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponseDto.successWithNoContent());
    }
}
