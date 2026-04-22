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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    @InjectMocks
    private ReviewService reviewService;

    @Mock private ReviewRepository reviewRepository;
    @Mock private DeveloperRepository developerRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private DeveloperService developerService;
    @Mock private ClientRepository clientRepository;
    @Mock private ProposalRepository proposalRepository;
    @Mock private ClientService clientService;

    private User createTestUser(Long id, String email, UserRole role) {
        User user = User.builder().name("User" + id).email(email).userRole(role).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("성공: Client가 Developer에게 리뷰 생성")
    void createReview_ClientToDeveloper_Success() {
        String email = "client@test.com";
        Long targetUserId = 2L;
        ReviewRequestDto request = new ReviewRequestDto(1L, 5, "Great Dev");

        User clientUser = createTestUser(1L, email, UserRole.CLIENT);
        User devUser = createTestUser(targetUserId, "dev@test.com", UserRole.DEVELOPER);

        Client client = mock(Client.class);
        given(client.getUser()).willReturn(clientUser);
        given(client.getId()).willReturn(10L);

        Developer developer = mock(Developer.class);
        given(developer.getUser()).willReturn(devUser);
        given(developer.getId()).willReturn(20L);

        Project project = mock(Project.class);
        given(project.getStatus()).willReturn(ProjectStatus.COMPLETED);
        given(project.getClient()).willReturn(client);
        given(project.getId()).willReturn(1L);
        given(project.getTitle()).willReturn("Title");

        Proposal proposal = mock(Proposal.class);
        given(proposal.getStatus()).willReturn(ProposalStatus.ACCEPTED);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(clientUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(devUser));
        given(projectRepository.findById(request.projectId())).willReturn(Optional.of(project));
        given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
        given(developerRepository.findByUser(devUser)).willReturn(Optional.of(developer));
        given(proposalRepository.findByProjectAndDeveloper(project, developer)).willReturn(Optional.of(proposal));
        given(reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(any(), any(), any(), eq(ReviewRole.CLIENT))).willReturn(false);

        Review review = Review.builder().developer(developer).client(client).project(project).writerRole(ReviewRole.CLIENT).rating(5).build();
        ReflectionTestUtils.setField(review, "id", 100L);
        given(reviewRepository.save(any(Review.class))).willReturn(review);

        ReviewDto result = reviewService.createReview(request, targetUserId, email);
        assertThat(result.id()).isEqualTo(100L);
        verify(developerService).updateRating(eq(20L));
    }

    @Test
    @DisplayName("성공: Developer가 Client에게 리뷰 생성")
    void createReview_DeveloperToClient_Success() {
        String email = "dev@test.com";
        Long targetUserId = 1L;
        ReviewRequestDto request = new ReviewRequestDto(1L, 4, "Good Client");

        User devUser = createTestUser(2L, email, UserRole.DEVELOPER);
        User clientUser = createTestUser(targetUserId, "client@test.com", UserRole.CLIENT);

        Developer dev = mock(Developer.class);
        given(dev.getUser()).willReturn(devUser);
        given(dev.getId()).willReturn(20L);

        Client client = mock(Client.class);
        given(client.getUser()).willReturn(clientUser);
        given(client.getId()).willReturn(10L);

        Project project = mock(Project.class);
        given(project.getStatus()).willReturn(ProjectStatus.CANCELLED);
        given(project.getId()).willReturn(1L);
        given(project.getTitle()).willReturn("Title");

        Proposal proposal = mock(Proposal.class);
        given(proposal.getStatus()).willReturn(ProposalStatus.ACCEPTED);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(devUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(clientUser));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(developerRepository.findByUser(devUser)).willReturn(Optional.of(dev));
        given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
        given(proposalRepository.findByProjectAndDeveloper(project, dev)).willReturn(Optional.of(proposal));
        given(reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(any(), any(), any(), eq(ReviewRole.DEVELOPER))).willReturn(false);

        Review review = Review.builder().developer(dev).client(client).project(project).writerRole(ReviewRole.DEVELOPER).rating(4).build();
        ReflectionTestUtils.setField(review, "id", 101L);
        given(reviewRepository.save(any(Review.class))).willReturn(review);

        ReviewDto result = reviewService.createReview(request, targetUserId, email);
        assertThat(result.id()).isEqualTo(101L);
        verify(clientService).updateRating(eq(10L));
    }

    @Test
    @DisplayName("실패: 이미 작성한 리뷰 존재")
    void createReview_AlreadyExists_Fail() {
        String email = "c@t.com";
        User clientUser = createTestUser(1L, email, UserRole.CLIENT);
        User devUser = createTestUser(2L, "d@t.com", UserRole.DEVELOPER);

        Client client = mock(Client.class);
        given(client.getUser()).willReturn(clientUser);
        Developer developer = mock(Developer.class);
        given(developer.getUser()).willReturn(devUser);
        Project p = mock(Project.class);
        given(p.getStatus()).willReturn(ProjectStatus.COMPLETED);
        given(p.getClient()).willReturn(client);
        Proposal proposal = mock(Proposal.class);
        given(proposal.getStatus()).willReturn(ProposalStatus.ACCEPTED);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(clientUser));
        given(userRepository.findById(2L)).willReturn(Optional.of(devUser));
        given(projectRepository.findById(1L)).willReturn(Optional.of(p));
        given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
        given(developerRepository.findByUser(devUser)).willReturn(Optional.of(developer));
        given(proposalRepository.findByProjectAndDeveloper(p, developer)).willReturn(Optional.of(proposal));
        given(reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(any(), any(), any(), any())).willReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(new ReviewRequestDto(1L, 5, "C"), 2L, email))
                .isInstanceOf(ReviewException.class);
    }

    @Test
    @DisplayName("성공: 수정 후 평점 재계산 (5.0 -> 3.0)")
    void updateReview_Success() {
        String email = "c@t.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        
        Developer dev = mock(Developer.class);
        given(dev.getId()).willReturn(20L);
        given(dev.getUser()).willReturn(mock(User.class));
        Client client = mock(Client.class);
        given(client.getUser()).willReturn(mock(User.class));
        Project p = mock(Project.class);
        given(p.getTitle()).willReturn("T");

        Review review = Review.builder().developer(dev).client(client).project(p).writerRole(ReviewRole.CLIENT).rating(5).build();
        ReflectionTestUtils.setField(review, "id", 1L);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        reviewService.updateReview(1L, new ReviewUpdateRequestDto(3, "New"), email);
        verify(developerService).updateRating(eq(20L));
    }

    @Test
    @DisplayName("성공: 삭제 후 평균 계산 (5.0, 1.0 중 하나 삭제 -> 1.0)")
    void deleteReview_CorrectAverage() {
        String email = "c@t.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        
        Developer dev = mock(Developer.class);
        given(dev.getId()).willReturn(20L);
        Client client = mock(Client.class);
        given(client.getId()).willReturn(10L);

        Review r1 = mock(Review.class);
        given(r1.getDeveloper()).willReturn(dev);
        given(r1.getClient()).willReturn(client);
        given(r1.getWriterRole()).willReturn(ReviewRole.CLIENT);
        given(r1.getRating()).willReturn(5);

        Review r2 = mock(Review.class);
        given(r2.getRating()).willReturn(1);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(reviewRepository.findById(1L)).willReturn(Optional.of(r1));

        reviewService.deleteReview(1L, email);
        verify(developerService).updateRating(eq(20L));
    }

    @Test
    @DisplayName("성공: 페이징 조회")
    void getReviews_Paging_Success() {
        PageRequest pr = PageRequest.of(0, 5);
        Developer dev = mock(Developer.class);
        given(dev.getId()).willReturn(1L);
        given(dev.getUser()).willReturn(createTestUser(1L, "d@t.com", UserRole.DEVELOPER));
        Client cl = mock(Client.class);
        given(cl.getId()).willReturn(1L);
        given(cl.getUser()).willReturn(createTestUser(2L, "c@t.com", UserRole.CLIENT));
        Project pj = mock(Project.class);
        given(pj.getId()).willReturn(1L);
        given(pj.getTitle()).willReturn("T");

        Review r = mock(Review.class);
        given(r.getDeveloper()).willReturn(dev);
        given(r.getClient()).willReturn(cl);
        given(r.getProject()).willReturn(pj);
        given(r.getRating()).willReturn(5);

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(mock(User.class)));
        given(reviewRepository.searchReviewsByDeveloper(anyLong(), any(), any(), any(), any())).willReturn(new PageImpl<>(List.of(r), pr, 1));

        Page<ReviewDto> res = reviewService.getReviewsByDeveloper(1L, null, null, null, 1, 5, "u@t.com");
        assertThat(res.getContent()).hasSize(1);
    }

    // 클라이언트 입장
    @Test
    @DisplayName("실패: 리뷰 생성 - 대상 유저 없음")
    void createReview_TargetUserNotFound() {
        // given
        String email = "client@test.com";
        User clientUser = createTestUser(1L, email, UserRole.CLIENT);
        ReviewRequestDto request = new ReviewRequestDto(1L, 5, "Great Dev");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(clientUser));
        given(userRepository.findById(2L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, 2L, email))
                .isInstanceOf(ReviewException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("실패: 리뷰 생성 - 제안서 상태가 ACCEPTED가 아니면 예외 발생")
    void createReview_ProposalNotAccepted() {
        // given
        String email = "client@test.com";
        User clientUser = createTestUser(1L, email, UserRole.CLIENT);
        User devUser = createTestUser(2L, "dev@test.com", UserRole.DEVELOPER);
        ReviewRequestDto request = new ReviewRequestDto(1L, 5, "Great Dev");

        Client client = mock(Client.class);
        given(client.getUser()).willReturn(clientUser);

        Developer developer = mock(Developer.class);

        Project project = mock(Project.class);
        given(project.getStatus()).willReturn(ProjectStatus.COMPLETED);
        given(project.getClient()).willReturn(client);

        Proposal proposal = mock(Proposal.class);
        given(proposal.getStatus()).willReturn(ProposalStatus.PENDING);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(clientUser));
        given(userRepository.findById(2L)).willReturn(Optional.of(devUser));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
        given(developerRepository.findByUser(devUser)).willReturn(Optional.of(developer));
        given(proposalRepository.findByProjectAndDeveloper(project, developer)).willReturn(Optional.of(proposal));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, 2L, email))
                .isInstanceOf(ReviewException.class)
                .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("성공: 관리자가 CLIENT 작성 리뷰를 삭제한 뒤 남은 리뷰가 없으면 개발자 평점은 0.0으로 재계산된다")
    void deleteReview_NoReviewsLeft_DeveloperRatingReset() {
        // given
        String email = "admin@test.com";
        User adminUser = createTestUser(99L, email, UserRole.ADMIN);

        Developer developer = mock(Developer.class);
        given(developer.getId()).willReturn(20L);

        Client client = mock(Client.class);
        given(client.getId()).willReturn(10L);

        Review review = mock(Review.class);
        given(review.getDeveloper()).willReturn(developer);
        given(review.getClient()).willReturn(client);
        given(review.getWriterRole()).willReturn(ReviewRole.CLIENT); // 이 줄 중요

        given(userRepository.findByEmail(email)).willReturn(Optional.of(adminUser));
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        reviewService.deleteReview(1L, email);

        // then
        verify(reviewRepository).delete(review);
        verify(reviewRepository).flush();
        verify(developerService).updateRating(20L);
    }


    // 개발자 입장
    @Test
    @DisplayName("실패: Developer가 Developer에게 리뷰 작성 시 예외 발생")
    void createReview_DeveloperToDeveloper_Forbidden() {
        // given
        String email = "dev@test.com";
        Long targetUserId = 3L;
        ReviewRequestDto request = new ReviewRequestDto(1L, 4, "Good");

        User loginDevUser = createTestUser(2L, email, UserRole.DEVELOPER);
        User targetDevUser = createTestUser(targetUserId, "other-dev@test.com", UserRole.DEVELOPER);

        Project project = mock(Project.class);
        given(project.getStatus()).willReturn(ProjectStatus.COMPLETED);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(loginDevUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(targetDevUser));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, targetUserId, email))
                .isInstanceOf(ReviewException.class)
                .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
    }


    @Test
    @DisplayName("실패: Developer가 이미 작성한 리뷰가 존재하면 예외 발생")
    void createReview_Developer_AlreadyExists() {
        // given
        String email = "dev@test.com";
        Long targetUserId = 1L;
        ReviewRequestDto request = new ReviewRequestDto(1L, 4, "Good Client");

        User devUser = createTestUser(2L, email, UserRole.DEVELOPER);
        User clientUser = createTestUser(targetUserId, "client@test.com", UserRole.CLIENT);

        Developer developer = mock(Developer.class);
        Client client = mock(Client.class);

        Project project = mock(Project.class);
        given(project.getStatus()).willReturn(ProjectStatus.COMPLETED);

        Proposal proposal = mock(Proposal.class);
        given(proposal.getStatus()).willReturn(ProposalStatus.ACCEPTED);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(devUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(clientUser));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(developerRepository.findByUser(devUser)).willReturn(Optional.of(developer));
        given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
        given(proposalRepository.findByProjectAndDeveloper(project, developer)).willReturn(Optional.of(proposal));
        given(reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(any(), any(), any(), eq(ReviewRole.DEVELOPER)))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, targetUserId, email))
                .isInstanceOf(ReviewException.class)
                .hasMessage(ErrorCode.REVIEW_ALREADY_EXISTS.getMessage());
    }

    @Test
    @DisplayName("성공: Developer가 리뷰 수정 시 Client 평점이 재계산된다")
    void updateReview_Developer_Success() {
        // given
        String email = "dev@test.com";
        User user = createTestUser(2L, email, UserRole.DEVELOPER);

        Developer developer = mock(Developer.class);
        Client client = mock(Client.class);
        given(client.getId()).willReturn(10L);
        given(client.getUser()).willReturn(createTestUser(1L, "client@test.com", UserRole.CLIENT));
        given(developer.getUser()).willReturn(createTestUser(2L, email, UserRole.DEVELOPER));

        Project project = mock(Project.class);
        given(project.getTitle()).willReturn("Project");

        Review review = Review.builder()
                .developer(developer)
                .client(client)
                .project(project)
                .writerRole(ReviewRole.DEVELOPER)
                .rating(5)
                .comment("Old")
                .build();
        ReflectionTestUtils.setField(review, "id", 1L);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(reviewRepository.findById(1L)).willReturn(Optional.of(review));

        // when
        reviewService.updateReview(1L, new ReviewUpdateRequestDto(3, "New"), email);

        // then
        verify(clientService).updateRating(eq(10L));
    }

    @Test
    @DisplayName("성공: Developer는 프로젝트 상태가 COMPLETED이면 Client에게 리뷰를 생성할 수 있다")
    void createReview_DeveloperToClient_ProjectCompleted_Success() {
        // given
        String email = "dev@test.com";
        Long targetUserId = 1L;
        ReviewRequestDto request = new ReviewRequestDto(1L, 4, "Good Client");

        User devUser = createTestUser(2L, email, UserRole.DEVELOPER);
        User clientUser = createTestUser(targetUserId, "client@test.com", UserRole.CLIENT);

        Developer developer = mock(Developer.class);
        given(developer.getId()).willReturn(20L);
        given(developer.getUser()).willReturn(devUser);

        Client client = mock(Client.class);
        given(client.getId()).willReturn(10L);
        given(client.getUser()).willReturn(clientUser);

        Project project = mock(Project.class);
        given(project.getStatus()).willReturn(ProjectStatus.COMPLETED);
        given(project.getId()).willReturn(1L);
        given(project.getTitle()).willReturn("Project");

        Proposal proposal = mock(Proposal.class);
        given(proposal.getStatus()).willReturn(ProposalStatus.ACCEPTED);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(devUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(clientUser));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(developerRepository.findByUser(devUser)).willReturn(Optional.of(developer));
        given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
        given(proposalRepository.findByProjectAndDeveloper(project, developer)).willReturn(Optional.of(proposal));
        given(reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(any(), any(), any(), eq(ReviewRole.DEVELOPER)))
                .willReturn(false);

        Review review = Review.builder()
                .developer(developer)
                .client(client)
                .project(project)
                .writerRole(ReviewRole.DEVELOPER)
                .rating(4)
                .comment("Good Client")
                .build();
        ReflectionTestUtils.setField(review, "id", 200L);

        given(reviewRepository.save(any(Review.class))).willReturn(review);

        // when
        ReviewDto result = reviewService.createReview(request, targetUserId, email);

        // then
        assertThat(result.id()).isEqualTo(200L);
        verify(clientService).updateRating(eq(10L));
    }

    @Test
    @DisplayName("성공: Developer는 프로젝트 상태가 CANCELLED이면 Client에게 리뷰를 생성할 수 있다")
    void createReview_DeveloperToClient_ProjectCancelled_Success() {
        // given
        String email = "dev@test.com";
        Long targetUserId = 1L;
        ReviewRequestDto request = new ReviewRequestDto(1L, 5, "Cancelled but communication was good");

        User devUser = createTestUser(2L, email, UserRole.DEVELOPER);
        User clientUser = createTestUser(targetUserId, "client@test.com", UserRole.CLIENT);

        Developer developer = mock(Developer.class);
        given(developer.getId()).willReturn(20L);
        given(developer.getUser()).willReturn(devUser);

        Client client = mock(Client.class);
        given(client.getId()).willReturn(10L);
        given(client.getUser()).willReturn(clientUser);

        Project project = mock(Project.class);
        given(project.getStatus()).willReturn(ProjectStatus.CANCELLED);
        given(project.getId()).willReturn(1L);
        given(project.getTitle()).willReturn("Cancelled Project");

        Proposal proposal = mock(Proposal.class);
        given(proposal.getStatus()).willReturn(ProposalStatus.ACCEPTED);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(devUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(clientUser));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(developerRepository.findByUser(devUser)).willReturn(Optional.of(developer));
        given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
        given(proposalRepository.findByProjectAndDeveloper(project, developer)).willReturn(Optional.of(proposal));
        given(reviewRepository.existsByProjectAndClientAndDeveloperAndWriterRole(any(), any(), any(), eq(ReviewRole.DEVELOPER)))
                .willReturn(false);

        Review review = Review.builder()
                .developer(developer)
                .client(client)
                .project(project)
                .writerRole(ReviewRole.DEVELOPER)
                .rating(5)
                .comment("Cancelled but communication was good")
                .build();
        ReflectionTestUtils.setField(review, "id", 201L);

        given(reviewRepository.save(any(Review.class))).willReturn(review);

        // when
        ReviewDto result = reviewService.createReview(request, targetUserId, email);

        // then
        assertThat(result.id()).isEqualTo(201L);
        verify(clientService).updateRating(eq(10L));
    }

    @Test
    @DisplayName("실패: Developer는 프로젝트 상태가 IN_PROGRESS이면 리뷰를 생성할 수 없다")
    void createReview_DeveloperToClient_ProjectInProgress_Forbidden() {
        // given
        String email = "dev@test.com";
        Long targetUserId = 1L;
        ReviewRequestDto request = new ReviewRequestDto(1L, 4, "Good Client");

        User devUser = createTestUser(2L, email, UserRole.DEVELOPER);
        User clientUser = createTestUser(targetUserId, "client@test.com", UserRole.CLIENT);

        Project project = mock(Project.class);
        given(project.getStatus()).willReturn(ProjectStatus.IN_PROGRESS);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(devUser));
        given(userRepository.findById(targetUserId)).willReturn(Optional.of(clientUser));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(request, targetUserId, email))
                .isInstanceOf(ReviewException.class)
                .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("성공: 관리자가 CLIENT 작성 리뷰를 삭제하면 개발자 평점이 재계산된다")
    void deleteReview_AdminDeletesClientReview_DeveloperRatingUpdated() {
        // given
        String email = "admin@test.com";
        User adminUser = createTestUser(99L, email, UserRole.ADMIN);

        Developer developer = mock(Developer.class);
        given(developer.getId()).willReturn(20L);

        Client client = mock(Client.class);
        given(client.getId()).willReturn(10L);

        Review deletedReview = mock(Review.class);
        given(deletedReview.getDeveloper()).willReturn(developer);
        given(deletedReview.getClient()).willReturn(client);
        given(deletedReview.getWriterRole()).willReturn(ReviewRole.CLIENT);

        Review remainReview = mock(Review.class);
        given(remainReview.getRating()).willReturn(4);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(adminUser));
        given(reviewRepository.findById(1L)).willReturn(Optional.of(deletedReview));

        // when
        reviewService.deleteReview(1L, email);

        // then
        verify(reviewRepository).delete(deletedReview);
        verify(reviewRepository).flush();
        verify(developerService).updateRating(20L);
        verifyNoInteractions(clientService);
    }


    @Test
    @DisplayName("성공: 관리자가 DEVELOPER 작성 리뷰를 삭제하면 클라이언트 평점이 재계산된다")
    void deleteReview_AdminDeletesDeveloperReview_ClientRatingUpdated() {
        // given
        String email = "admin@test.com";
        User adminUser = createTestUser(99L, email, UserRole.ADMIN);

        Developer developer = mock(Developer.class);
        given(developer.getId()).willReturn(20L);

        Client client = mock(Client.class);
        given(client.getId()).willReturn(10L);

        Review deletedReview = mock(Review.class);
        given(deletedReview.getDeveloper()).willReturn(developer);
        given(deletedReview.getClient()).willReturn(client);
        given(deletedReview.getWriterRole()).willReturn(ReviewRole.DEVELOPER);

        Review remainReview = mock(Review.class);
        given(remainReview.getRating()).willReturn(5);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(adminUser));
        given(reviewRepository.findById(1L)).willReturn(Optional.of(deletedReview));

        // when
        reviewService.deleteReview(1L, email);

        // then
        verify(reviewRepository).delete(deletedReview);
        verify(reviewRepository).flush();
        verify(clientService).updateRating(10L);
        verifyNoInteractions(developerService);
    }




}
