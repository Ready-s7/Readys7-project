package com.example.readys7project.domain.review.service;

import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.developer.service.DeveloperService;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequest;
import com.example.readys7project.domain.review.entity.Review;
import com.example.readys7project.domain.review.repository.ReviewRepository;
import com.example.readys7project.domain.user.entity.User;
import com.example.readys7project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DeveloperRepository developerRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DeveloperService developerService;

    @Transactional
    public ReviewDto createReview(ReviewRequest request, String userEmail) {
        User client = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (client.getRole() != User.UserRole.CLIENT) {
            throw new RuntimeException("클라이언트만 리뷰를 작성할 수 있습니다");
        }

        Developer developer = developerRepository.findById(request.getDeveloperId())
                .orElseThrow(() -> new RuntimeException("개발자를 찾을 수 없습니다"));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("프로젝트를 찾을 수 없습니다"));

        Review review = Review.builder()
                .developer(developer)
                .client(client)
                .project(project)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);

        // 개발자 평점 업데이트
        updateDeveloperRating(developer.getId());

        return convertToDto(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByDeveloper(Long developerId) {
        return reviewRepository.findByDeveloperId(developerId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsByProject(Long projectId) {
        return reviewRepository.findByProjectId(projectId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private void updateDeveloperRating(Long developerId) {
        List<Review> reviews = reviewRepository.findByDeveloperId(developerId);

        if (reviews.isEmpty()) {
            return;
        }

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // 소수점 첫째자리까지
        averageRating = Math.round(averageRating * 10.0) / 10.0;

        developerService.updateRating(developerId, averageRating, reviews.size());
    }

    private ReviewDto convertToDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .developerId(review.getDeveloper().getId())
                .developerName(review.getDeveloper().getUser().getName())
                .clientId(review.getClient().getId())
                .clientName(review.getClient().getName())
                .projectId(review.getProject().getId())
                .projectTitle(review.getProject().getTitle())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
