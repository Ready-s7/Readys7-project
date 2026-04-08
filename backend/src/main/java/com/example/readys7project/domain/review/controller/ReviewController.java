package com.example.readys7project.domain.review.controller;

import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequest;
import com.example.readys7project.domain.review.service.ReviewService;
import com.example.readys7project.global.dto.ApiResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<ReviewDto>> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.CREATED, reviewService.createReview(request, email)));
    }

    @GetMapping("/developer/{developerId}")
    public ResponseEntity<ApiResponseDto<List<ReviewDto>>> getReviewsByDeveloper(@PathVariable Long developerId) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.getReviewsByDeveloper(developerId)));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponseDto<List<ReviewDto>>> getReviewsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, reviewService.getReviewsByProject(projectId)));
    }
}
