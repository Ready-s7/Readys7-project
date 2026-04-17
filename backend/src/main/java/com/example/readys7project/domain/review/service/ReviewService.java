package com.example.readys7project.domain.review.service;


import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.review.dto.ReviewDto;
import com.example.readys7project.domain.review.dto.request.ReviewRequestDto;
import com.example.readys7project.domain.review.dto.request.ReviewUpdateRequestDto;
import com.example.readys7project.domain.review.entity.Review;
import com.example.readys7project.domain.review.enums.ReviewRole;
import com.example.readys7project.domain.review.repository.ReviewRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.client.service.ClientService;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.developer.service.DeveloperService;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ReviewException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final DeveloperRepository developerRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final DeveloperService developerService;
    private final ClientRepository clientRepository;
    private final ProposalRepository proposalRepository;
    private final ClientService clientService;


    // 리뷰 생성
    @Transactional
    public ReviewDto createReview(ReviewRequestDto request, Long targetUserId, String email) {
        User user = findUserByEmail(email);
        validateReviewWriterRole(user);

        User targetUser = findUserById(targetUserId);
        Project project = findProject(request.projectId());

        Client client;
        Developer developer;

        if (project.getStatus() != ProjectStatus.COMPLETED
                && project.getStatus() != ProjectStatus.CANCELLED) {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }

        if (user.getUserRole() == UserRole.CLIENT) {
            if (targetUser.getUserRole() != UserRole.DEVELOPER) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            if (!project.getClient().getUser().equals(user)) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            client = clientRepository.findByUser(user)
                    .orElseThrow(() -> new ReviewException(ErrorCode.CLIENT_NOT_FOUND));

            developer = developerRepository.findByUser(targetUser)
                    .orElseThrow(() -> new ReviewException(ErrorCode.DEVELOPER_NOT_FOUND));

            Proposal proposal = proposalRepository.findByProjectAndDeveloper(project, developer)
                    .orElseThrow(() -> new ReviewException(ErrorCode.PROPOSAL_NOT_FOUND));

            if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            boolean alreadyExists = reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(
                    project, client, developer, ReviewRole.CLIENT);

            if (alreadyExists) {
                throw new ReviewException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }

            Review review = Review.builder()
                    .developer(developer).client(client).project(project)
                    .writerRole(ReviewRole.CLIENT).rating(request.rating()).comment(request.comment()).build();

            Review savedReview = reviewRepository.save(review);
            updateDeveloperRating(developer.getId());
            return convertToDto(savedReview);
        }


        if (user.getUserRole() == UserRole.DEVELOPER) {
            if (targetUser.getUserRole() != UserRole.CLIENT) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            developer = developerRepository.findByUser(user)
                    .orElseThrow(() -> new ReviewException(ErrorCode.DEVELOPER_NOT_FOUND));

            client = clientRepository.findByUser(targetUser)
                    .orElseThrow(() -> new ReviewException(ErrorCode.CLIENT_NOT_FOUND));

            Proposal proposal = proposalRepository.findByProjectAndDeveloper(project, developer)
                    .orElseThrow(() -> new ReviewException(ErrorCode.PROPOSAL_NOT_FOUND));

            if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            boolean alreadyExists = reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(
                    project, client, developer, ReviewRole.DEVELOPER);

            if (alreadyExists) {
                throw new ReviewException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }

            Review review = Review.builder()
                    .developer(developer).client(client).project(project)
                    .writerRole(ReviewRole.DEVELOPER).rating(request.rating()).comment(request.comment()).build();

            Review savedReview = reviewRepository.save(review);
            updateClientRating(client.getId());
            return convertToDto(savedReview);
        }

        throw new ReviewException(ErrorCode.USER_FORBIDDEN);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsByDeveloper(Long developerId, Integer rating, Integer minRating, Integer maxRating, int page, int size, String email) {
        findUserByEmail(email);
        Pageable pageable = PageRequest.of(page - 1, size);
        return reviewRepository.searchReviewsByDeveloper(developerId, rating, minRating, maxRating, pageable).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsByClient(Long clientId, Integer rating, Integer minRating, Integer maxRating, int page, int size, String email) {
        findUserByEmail(email);
        Pageable pageable = PageRequest.of(page - 1, size);
        return reviewRepository.searchReviewsByClient(clientId, rating, minRating, maxRating, pageable).map(this::convertToDto);
    }

    /**
     * 리뷰 수정 (AOP에서 소유권 검증 완료)
     */
    @Transactional
    public ReviewDto updateReview(Long reviewId, @Valid ReviewUpdateRequestDto request, String email) {
        validateUpdateRequest(request);
        User user = findUserByEmail(email);
        Review review = findReview(reviewId);

        review.updateReview(request);

        if (user.getUserRole() == UserRole.CLIENT) {
            updateDeveloperRating(review.getDeveloper().getId());
        } else if (user.getUserRole() == UserRole.DEVELOPER) {
            updateClientRating(review.getClient().getId());
        }

        return convertToDto(review);
    }

    /**
     * 리뷰 삭제 (AOP에서 소유권/관리자 검증 완료)
     */
    @Transactional
    public void deleteReview(Long reviewId, String email) {
        User user = findUserByEmail(email);
        Review review = findReview(reviewId);

        Long developerId = review.getDeveloper().getId();
        Long clientId = review.getClient().getId();
        UserRole targetUser = user.getUserRole();

        reviewRepository.delete(review);
        reviewRepository.flush();

        if (targetUser == UserRole.CLIENT) {
            updateDeveloperRating(developerId);
        } else if (targetUser == UserRole.DEVELOPER) {
            updateClientRating(clientId);
        }
    }

    private void validateUpdateRequest(ReviewUpdateRequestDto request){
        if (request.rating() == null && (request.comment() == null || request.comment().isBlank())) {
            throw new ReviewException(ErrorCode.REVIEW_UPDATE_DATA_NULL);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));
    }

    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new ReviewException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private void validateReviewWriterRole(User user) {
        if (user.getUserRole() != UserRole.CLIENT && user.getUserRole() != UserRole.DEVELOPER) {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }
    }

    private ReviewDto convertToDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId()).developerId(review.getDeveloper().getId())
                .developerName(review.getDeveloper().getUser().getName())
                .clientId(review.getClient().getId()).clientName(review.getClient().getUser().getName())
                .projectId(review.getProject().getId()).projectTitle(review.getProject().getTitle())
                .writerRole(review.getWriterRole()).rating(review.getRating())
                .comment(review.getComment()).createdAt(review.getCreatedAt()).build();
    }

    private void updateDeveloperRating(Long developerId) {
        List<Review> reviews = reviewRepository.findByDeveloperId(developerId);
        if (reviews.isEmpty()) {
            developerService.updateRating(developerId, 0.0, 0);
            return;
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        avg = Math.round(avg * 10.0) / 10.0;
        developerService.updateRating(developerId, avg, reviews.size());
    }

    private void updateClientRating(Long clientId) {
        List<Review> reviews = reviewRepository.findByClientId(clientId);
        if (reviews.isEmpty()) {
            clientService.updateRating(clientId, 0.0, 0);
            return;
        }
        double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        avg = Math.round(avg * 10.0) / 10.0;
        clientService.updateRating(clientId, avg, reviews.size());
    }
}
