package com.example.readys7project.domain.review.controller;

import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequest;
import com.example.readys7project.domain.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<ReviewDto> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(reviewService.createReview(request, email));
    }

    @GetMapping("/developer/{developerId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByDeveloper(@PathVariable Long developerId) {
        return ResponseEntity.ok(reviewService.getReviewsByDeveloper(developerId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(reviewService.getReviewsByProject(projectId));
    }
}
