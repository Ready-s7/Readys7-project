package com.example.readys7project.domain.review.controller;

import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequestDto;
import com.example.readys7project.domain.review.dto.request.ReviewUpdateRequestDto;
import com.example.readys7project.domain.review.service.ReviewService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping("/v1/reviews")
    public ResponseEntity<ApiResponseDto<ReviewDto>> createReview(
            @Valid @RequestBody ReviewRequestDto request,
            @RequestParam Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.CREATED, reviewService.createReview(request,targetUserId,email)));
    }

    // 개발자 조회
    //  구분선 params 추가
    @GetMapping(value = "/v1/reviews", params = "developerId")
    public ResponseEntity<ApiResponseDto<Page<ReviewDto>>> getReviewsByDeveloper(
            @RequestParam Long developerId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.getReviewsByDeveloper(developerId, rating, minRating, maxRating, page, size,email)));
    }


    // 클라이언트 조회
    // . 구분선 params 추가
    @GetMapping(value = "/v1/reviews", params = "clientId")
    public ResponseEntity<ApiResponseDto<Page<ReviewDto>>> getReviewsByClient(
            @RequestParam Long clientId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    )
    {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.getReviewsByClient(clientId, rating, minRating, maxRating, page, size,email)));
    }

    // 리뷰 수정
    @PatchMapping("/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponseDto<ReviewDto>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.updateReview(reviewId,request,email)));

    }


    // 리뷰 삭제
    @DeleteMapping("/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponseDto<ReviewDto>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ){
        String email = customUserDetails.getEmail();
        reviewService.deleteReview(reviewId,email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponseDto.successWithNoContent());

    }

}
