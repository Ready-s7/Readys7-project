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
import com.example.readys7project.domain.review.repository.ReviewRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
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
import java.util.stream.Collectors;

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


    // 리뷰 생성 로직
    // 1. 리뷰 생성 가능
    // 프로젝트 상태가 완료/중단
    // 2. 리뷰 생성 불가능
    // 프로젝트 상태가 작업중/오픈

    // 프로젝트 검증 필용
    // 리뷰 생성 비즈니스 로직

    @Transactional
    public ReviewDto createReview(ReviewRequestDto request, Long targetUserId , String userEmail) {

        // 작성자 존재 검증
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        // 리뷰를 받는 대상 사용자가 실제 DB에 존재하는가
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        // 프로젝트 조회
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ReviewException(ErrorCode.PROJECT_NOT_FOUND));


        // 프로젝트 상태 검증
        // 프로젝트 상태가 작업 완료, 작업 취소일때 만 가능. 나중에 작업 취소는 작업 중단 상태로 수정 예정.
        if (project.getStatus() != ProjectStatus.COMPLETED
                && project.getStatus() != ProjectStatus.CANCELLED) {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }

        // 리뷰를 작성하는 사람이 클라이언트인데
        // 프로젝트를 발안한 당사자가 아니라면 에러 처리
        if (user.getUserRole().equals(UserRole.CLIENT) && !project.getClient().getUser().equals(user)) {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }

        // 리뷰를 작성하는 사람이 개발자인데
        // 해당 프로젝트에 제안서를 제출한 바 있으며
        // 해당 제안서가 허가를 받았는지 검증
        if (user.getUserRole().equals(UserRole.DEVELOPER)) {
            Developer loginDeveloper = developerRepository.findByUser(user)
                    .orElseThrow(() -> new ReviewException(ErrorCode.DEVELOPER_NOT_FOUND));

            Proposal proposal = proposalRepository.findByProjectAndDeveloper(project, loginDeveloper).orElseThrow(
                    () -> new ReviewException(ErrorCode.PROPOSAL_NOT_FOUND)
            );

            if (!proposal.getStatus().equals(ProposalStatus.ACCEPTED)) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
        }

        // 리뷰 작성자와 리뷰 대상자의 역할 조합이 올바른지 검증하는 로직
        // 작성자가 CLIENT 인 경우. CLIENT -> DEVELOPER 조합만 허용.
        if (user.getUserRole() == UserRole.CLIENT) {
            if (targetUser.getUserRole() != UserRole.DEVELOPER) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
            //  작성자가 DEVELOPER 인 경우
            //  DEVELOPER -> CLIENT 조합만 허용.
        } else if (user.getUserRole() == UserRole.DEVELOPER) {
            if (targetUser.getUserRole() != UserRole.CLIENT) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
        } else {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }

        Client client;
        Developer developer;
        Review savedReview;

        if (user.getUserRole() == UserRole.CLIENT) {
            client = clientRepository.findByUser(user)
                    .orElseThrow(() -> new ReviewException(ErrorCode.CLIENT_NOT_FOUND));

            developer = developerRepository.findByUser(targetUser)
                    .orElseThrow(() -> new ReviewException(ErrorCode.DEVELOPER_NOT_FOUND));

            Review review = Review.builder()
                    .developer(developer)
                    .client(client)
                    .project(project)
                    .rating(request.rating())
                    .comment(request.comment())
                    .build();

            savedReview = reviewRepository.save(review);

        } else if (user.getUserRole() == UserRole.DEVELOPER) {
            client = clientRepository.findByUser(targetUser)
                    .orElseThrow(() -> new ReviewException(ErrorCode.CLIENT_NOT_FOUND));

            developer = developerRepository.findByUser(user)
                    .orElseThrow(() -> new ReviewException(ErrorCode.DEVELOPER_NOT_FOUND));

            Review review = Review.builder()
                    .developer(developer)
                    .client(client)
                    .project(project)
                    .rating(request.rating())
                    .comment(request.comment())
                    .build();

            savedReview = reviewRepository.save(review);

        } else {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }

        // 작성자가 클라이언트 였다면 대상은 개발자 이므로 개발자 평점 갱신
        // 작성자가 개발자 였다면 대상은 클라이언트 이므로 클라이언트 평점 갱신
        if(user.getUserRole()==UserRole.CLIENT){
            updateDeveloperRating(developer.getId());
        }else if(user.getUserRole()==UserRole.DEVELOPER){
            updateClientRating(client.getId());
        }

        return convertToDto(savedReview);
    }


    // 특정 개발자가 받은 리뷰 목록 조회
    // 특정 개발자의 클라이언트가 남긴 리뷰 조회
    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsByDeveloper(
            Long developerId,
            Integer rating,
            Integer minRating,
            Integer maxRating,
            int page,
            int size,
            String email
    ) {

        // 검증 로직
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        Developer developer =developerRepository.findById(developerId).orElseThrow(
                ()-> new ReviewException(ErrorCode.DEVELOPER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page - 1, size);

        return reviewRepository.searchReviewsByDeveloper(developerId, rating, minRating, maxRating, pageable)
                .map(this::convertToDto);
    }


    // 클라이언트에 개발자가 남긴 리뷰 목록
    // 클라이언트 리뷰 목록 조회
    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsByClient
    (  Long clientId,
       Integer rating,
       Integer minRating,
       Integer maxRating,
       int page,
       int size,
       String email
    ) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        // 클라이언트 조회 검증
        Client client=clientRepository.findById(clientId).orElseThrow(
                ()->new ReviewException(ErrorCode.CLIENT_NOT_FOUND)
        );

        Pageable pageable = PageRequest.of(page - 1, size);


        return reviewRepository.searchReviewsByClient(clientId, rating, minRating, maxRating, pageable)
                .map(this::convertToDto);
    }



// 리뷰 평점 계산기. 개발자용
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

    // 리뷰 평점 계산기. 클라이언트 용.
    private void updateClientRating(Long clientId) {
        List<Review> reviews = reviewRepository.findByDeveloperId(clientId);

        if (reviews.isEmpty()) {
            return;
        }

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // 소수점 첫째자리까지
        averageRating = Math.round(averageRating * 10.0) / 10.0;

        developerService.updateRating(clientId, averageRating, reviews.size());
    }


    private ReviewDto convertToDto(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .developerId(review.getDeveloper().getId())
                .developerName(review.getDeveloper().getUser().getName())
                .clientId(review.getClient().getId())
                .clientName(review.getClient().getUser().getName())
                .projectId(review.getProject().getId())
                .projectTitle(review.getProject().getTitle())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }


    // 리뷰 수정
    @Transactional
    public ReviewDto updateReview(Long reviewId, @Valid ReviewUpdateRequestDto request, String email) {

        // 사용자 검증
        User user= userRepository.findByEmail(email)
                .orElseThrow(()->new ReviewException(ErrorCode.USER_NOT_FOUND));

        // 리뷰 존재 검증
        Review review =reviewRepository.findById(reviewId)
                .orElseThrow(()->new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        // 클라이언트 자기 자신이어야만 수정 가능.
        if(user.getUserRole()==UserRole.CLIENT){
            if(!review.getClient().getUser().equals(user)){
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
            // 개발자 자기 자신이여야만 수정 가능.
        } else if(user.getUserRole()==UserRole.DEVELOPER){
            if(!review.getDeveloper().getUser().equals(user)){
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
        } else {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }

        review.update(request.rating(), request.comment());

        Review updatedReview = reviewRepository.save(review);


        // 작성자가 클라이언트 였다면 대상은 개발자 이므로 개발자 평점 갱신
        // 작성자가 개발자 였다면 대상은 클라이언트 이므로 클라이언트 평점 갱신
        if (user.getUserRole() == UserRole.CLIENT) {
            updateDeveloperRating(review.getDeveloper().getId());
        } else if (user.getUserRole() == UserRole.DEVELOPER) {
            updateClientRating(review.getClient().getId());
        }

        return convertToDto(updatedReview);
    }

    /**
     1. 로그인 사용자 검증
     2. 삭제 대상 리뷰 존재 검증
     3. 리뷰 작성자 본인 여부 검증
     4. 삭제 후 평점 재계산에 필요한 정보 확보
     5. 리뷰 삭제
     6. 삭제 후 평점 재계산

     즉, 아무나 남의 리뷰를 삭제 할 수 없다. 리뷰 작성자만 자기 리뷰를 삭제할 수 있음
     리뷰가 삭제되면 대상의 평균 평점도 다시 조정된다.
     */
    @Transactional
    public void deleteReview(Long reviewId, String email) {

        User user=userRepository.findByEmail(email)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));

        Review review=reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));

        if (user.getUserRole() == UserRole.CLIENT) {
            if (!review.getClient().getUser().equals(user)) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
        } else if (user.getUserRole() == UserRole.DEVELOPER) {
            if (!review.getDeveloper().getUser().equals(user)) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
        } else {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }

        // 삭제 전 정보 꺼내기.
        // 이 리뷰를 누가 작성했는지에 따라 필요.
        // 클라이언트, 개발자.
        Long developerId=review.getDeveloper().getId();
        Long clientId=review.getClient().getId();
        UserRole targetUser=user.getUserRole();

        reviewRepository.delete(review);

        if (targetUser==UserRole.CLIENT){
            updateDeveloperRating(developerId);
        }else if (targetUser==UserRole.DEVELOPER){
            updateClientRating(clientId);
        }
    }
}
