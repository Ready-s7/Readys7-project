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
    private final ClientService clientService;


    // 리뷰 생성 비즈니스 로직
    @Transactional
    public ReviewDto createReview(ReviewRequestDto request, Long targetUserId, String email) {

        // 작성자 존재 검증
        User user = findUserByEmail(email);
        validateReviewWriterRole(user);

        // 리뷰를 받는 대상 사용자가 실제 DB에 존재하는가
        User targetUser = findUserById(targetUserId);
        // 프로젝트 조회
        Project project = findProject(request.projectId());

        // 클라인트, 개발자 객체 생성.
        Client client;
        Developer developer;

        // 전제 조건.
        // 리뷰는 완료 또는 취소된 프로젝트에서만 작성 가능하다.
        if (project.getStatus() != ProjectStatus.COMPLETED
                && project.getStatus() != ProjectStatus.CANCELLED) {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }


        // 만약 클라이언트라가 리뷰를 작성하는 경우
        if (user.getUserRole() == UserRole.CLIENT) {

            // 리뷰 대상자는 개발자여야 한다.
            if (targetUser.getUserRole() != UserRole.DEVELOPER) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            // 로그인한 클라이언트가 이 프로젝트의 실제 당사인지 검증한다.
            if (!project.getClient().getUser().equals(user)) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            // 이제 로그인한 사용자와 리뷰 대상 사용자를 각각 엔티티로 조횐한다.
            client = clientRepository.findByUser(user)
                    .orElseThrow(() -> new ReviewException(ErrorCode.CLIENT_NOT_FOUND));

            developer = developerRepository.findByUser(targetUser)
                    .orElseThrow(() -> new ReviewException(ErrorCode.DEVELOPER_NOT_FOUND));


            // 리뷰 대상 개발자가 이 프로젝트에 실제 참여한 개발자인지 검증한다.
            // 즉, 해당 프로젝트에 대한 제안서가 존재해야 한다.
            Proposal proposal = proposalRepository.findByProjectAndDeveloper(project, developer)
                    .orElseThrow(() -> new ReviewException(ErrorCode.PROPOSAL_NOT_FOUND));

            // 제안서가 승인(ACCEPTED)된 상태여야만 리뷰 작성이 가능하다.
            if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            // 같은 프로젝트에서 클라이언트가 이미 리뷰를 작성했는지 확인.
            // 이때 위에서 자기가 완료한 프로젝트, 제안서 검증을 하기 때문에. 클라이언트 작성 리뷰가 이미 존재하는지만 확인하면 된다.
            // 존재 하면 true발생 따라서 예외가 발생한다.
            boolean alreadyExists = reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(
                    project,
                    client,
                    developer,
                    ReviewRole.CLIENT
            );

            if (alreadyExists) {
                throw new ReviewException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }


            Review review = Review.builder()
                    .developer(developer)
                    .client(client)
                    .project(project)
                    .writerRole(ReviewRole.CLIENT)
                    .rating(request.rating())
                    .comment(request.comment())
                    .build();

            Review savedReview = reviewRepository.save(review);

            // 클라이언트가 개발자에게 남긴 리뷰이므로 개발자의 평점을 갱신한다.
            updateDeveloperRating(developer.getId());
            return convertToDto(savedReview);
        }


        // 만약 개발자가 리뷰를 작성하는 경우
        if (user.getUserRole() == UserRole.DEVELOPER) {

            // 이때 리뷰를 받은 대상이 클라이언트이어야 한다.
            if (targetUser.getUserRole() != UserRole.CLIENT) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }


            //  로그인한 사용자와 리뷰 대상 사용자를 각각 엔티티로 조회한다.
            developer = developerRepository.findByUser(user)
                    .orElseThrow(() -> new ReviewException(ErrorCode.DEVELOPER_NOT_FOUND));

            client = clientRepository.findByUser(targetUser)
                    .orElseThrow(() -> new ReviewException(ErrorCode.CLIENT_NOT_FOUND));


            // 로그인한 개발자가 이 프로젝트에 실제 참여한 개발자인지 검증한다.
            // 즉, 해당 프로젝트에 대한 제안서가 존재해야 한다.
            Proposal proposal = proposalRepository.findByProjectAndDeveloper(project, developer)
                    .orElseThrow(() -> new ReviewException(ErrorCode.PROPOSAL_NOT_FOUND));


            // 이때 제안서의 상태가 승인 상태여야 한다.
            if (proposal.getStatus() != ProposalStatus.ACCEPTED) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            // 같은 프로젝트에서 개발자가 이미 리뷰를 작성했는지 확인.
            // 이때 위에서 자기가 완료한 프로젝트, 제안서 검증을 하기 때문에. 개발자 작성 리뷰가 이미 존재하는지만 확인하면 된다.
            // 존재 하면 true발생 따라서 예외가 발생한다.
            boolean alreadyExists = reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(
                    project,
                    client,
                    developer,
                    ReviewRole.DEVELOPER
            );

            if (alreadyExists) {
                throw new ReviewException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }


            // 작성자 역할을 DEVELOPER로 저장하고 리뷰를 생성한다.
            Review review = Review.builder()
                    .developer(developer)
                    .client(client)
                    .project(project)
                    .writerRole(ReviewRole.DEVELOPER)
                    .rating(request.rating())
                    .comment(request.comment())
                    .build();

            Review savedReview = reviewRepository.save(review);

            // 개발자가 클라이언트에게 남긴 리뷰이므로 클라이언트 평점을 갱신한다.
            updateClientRating(client.getId());
            return convertToDto(savedReview);
        }

        throw new ReviewException(ErrorCode.USER_FORBIDDEN);
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

        // 로그인만 하면 다 볼 수 있다.
        findUserByEmail(email);

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

        // 로그인만 하면 다 볼 수 있다.
        findUserByEmail(email);

        // 클라이언트 조회 검증
        Client client=clientRepository.findById(clientId).orElseThrow(
                ()->new ReviewException(ErrorCode.CLIENT_NOT_FOUND)
        );

        Pageable pageable = PageRequest.of(page - 1, size);


        return reviewRepository.searchReviewsByClient(clientId, rating, minRating, maxRating, pageable)
                .map(this::convertToDto);
    }

    // 리뷰 수정
    @Transactional
    public ReviewDto updateReview(Long reviewId, @Valid ReviewUpdateRequestDto request, String email) {
        validateUpdateRequest(request);

        User user = findUserByEmail(email);
        validateReviewWriterRole(user);

        Review review = findReview(reviewId);
        validateReviewOwner(user, review);

        review.updateReview(request);

        if (user.getUserRole() == UserRole.CLIENT) {
            updateDeveloperRating(review.getDeveloper().getId());
        } else if (user.getUserRole() == UserRole.DEVELOPER) {
            updateClientRating(review.getClient().getId());
        }

        return convertToDto(review);
    }

    /**
     * 공통 메서드들
     */
    // rating만 보내도 됨
    // comment만 보내도 됨
    // 둘다 안보내면 안됨.
    private void validateUpdateRequest(ReviewUpdateRequestDto request){
        if (request.rating() == null && (request.comment() == null || request.comment().isBlank())) {
            throw new ReviewException(ErrorCode.REVIEW_UPDATE_DATA_NULL);
        }

    }
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));
    }


    // 이메일 검증.
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ReviewException(ErrorCode.USER_NOT_FOUND));
    }

    // 리뷰 검증.
    private Review findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(ErrorCode.REVIEW_NOT_FOUND));
    }

    // 프로젝트 검증
    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ReviewException(ErrorCode.PROJECT_NOT_FOUND));
    }

   // 유저 역할 검증.
    private void validateReviewWriterRole(User user) {
        if (user.getUserRole() != UserRole.CLIENT && user.getUserRole() != UserRole.DEVELOPER) {
            throw new ReviewException(ErrorCode.USER_FORBIDDEN);
        }
    }

    // 리뷰 수정할때 역할은 자기거랑 맞아야함.
    // 클라 -> 클라
    // 개발자 -> 개발자
    private void validateReviewOwner(User user, Review review) {
        // 클라이언트가 수정/삭제하려면
        // 해당 리뷰가 CLIENT가 작성한 리뷰여야 하고,
        // 로그인한 사용자가 그 리뷰의 client.user 와 일치해야 한다.
        if (user.getUserRole() == UserRole.CLIENT) {
            if (review.getWriterRole() != ReviewRole.CLIENT) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            if (!review.getClient().getUser().equals(user)) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
        }

        // 개발자가 수정/삭제하려면
        // 해당 리뷰가 DEVELOPER가 작성한 리뷰여야 하고,
        // 로그인한 사용자가 그 리뷰의 developer.user 와 일치해야 한다.
        if (user.getUserRole() == UserRole.DEVELOPER) {
            if (review.getWriterRole() != ReviewRole.DEVELOPER) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }

            if (!review.getDeveloper().getUser().equals(user)) {
                throw new ReviewException(ErrorCode.USER_FORBIDDEN);
            }
        }
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

        User user = findUserByEmail(email);
        Review review = findReview(reviewId);

        // 관리자는 삭제가능
        if (user.getUserRole() != UserRole.ADMIN) {
            validateReviewWriterRole(user);
            validateReviewOwner(user, review);
        }

        // 삭제 전 정보 꺼내기.
        // 이 리뷰를 누가 작성했는지에 따라 필요.
        // 클라이언트, 개발자.
        Long developerId=review.getDeveloper().getId();
        Long clientId=review.getClient().getId();
        UserRole targetUser=user.getUserRole();

        reviewRepository.delete(review);
        reviewRepository.flush();

        if (targetUser==UserRole.CLIENT){
            updateDeveloperRating(developerId);
        }else if (targetUser==UserRole.DEVELOPER){
            updateClientRating(clientId);
        }
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
                .writerRole(review.getWriterRole())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    // 리뷰 평점 계산기. 개발자용
    private void updateDeveloperRating(Long developerId) {
        List<Review> reviews = reviewRepository.findByDeveloperId(developerId);

        if (reviews.isEmpty()) {
            return; // 리뷰가 0개면 그냥 return!
        }

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // 소수점 첫째자리까지
        averageRating = Math.round(averageRating * 10.0) / 10.0;

        developerService.updateRating(developerId, averageRating, reviews.size());

        if (reviews.isEmpty()) {
            developerService.updateRating(developerId, 0.0, 0); // ← 이렇게 수정 필요!
        }

    }

    // 리뷰 평점 계산기. 클라이언트 용.
    private void updateClientRating(Long clientId) {
        List<Review> reviews = reviewRepository.findByClientId(clientId);


        if (reviews.isEmpty()) {
            return; // 리뷰가 0개면 그냥 return!
        }

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);

        // 소수점 첫째자리까지
        averageRating = Math.round(averageRating * 10.0) / 10.0;

        clientService.updateRating(clientId, averageRating, reviews.size());

        if (reviews.isEmpty()) {
            clientService.updateRating(clientId, 0.0, 0); //
        }
    }
}
