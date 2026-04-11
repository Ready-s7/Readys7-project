package com.example.readys7project.domain.review.controller;

import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequest;
import com.example.readys7project.domain.review.dto.request.ReviewUpdateRequest;
import com.example.readys7project.domain.review.service.ReviewService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 리뷰 생성
    @PostMapping("/v1/reviews")
    public ResponseEntity<ApiResponseDto<ReviewDto>> createReview(
            @Valid @RequestBody ReviewRequest request,
            @RequestParam Long targetUserId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.CREATED, reviewService.createReview(request,targetUserId,email)));
    }

    // 비 로그인도 가능.  구분선 params 추가
    @GetMapping(value = "/v1/reviews", params = "developerId")
    public ResponseEntity<ApiResponseDto<List<ReviewDto>>> getReviewsByDeveloper(@RequestParam Long developerId) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.getReviewsByDeveloper(developerId)));
    }

    // 비 로그인도 가능. 구분선 params 추가
    @GetMapping(value = "/v1/reviews", params = "clientId")
    public ResponseEntity<ApiResponseDto<List<ReviewDto>>> getReviewsByClient(@RequestParam Long clientId) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.getReviewsByClient(clientId)));
    }

    // 리뷰 수정
    @PatchMapping("/v1/reviews/{reviewId}")
    public ResponseEntity<ApiResponseDto<ReviewDto>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request,
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
