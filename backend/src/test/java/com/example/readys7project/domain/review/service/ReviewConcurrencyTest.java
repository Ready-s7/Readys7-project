package com.example.readys7project.domain.review.service;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.review.dto.request.ReviewRequestDto;
import com.example.readys7project.domain.review.repository.ReviewRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.enums.ParticipateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("bulk")
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewConcurrencyTest {

    @Autowired UserRepository userRepository;
    @Autowired DeveloperRepository developerRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired ProposalRepository proposalRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ReviewTransactionService reviewTransactionService;
    @Autowired CategoryRepository categoryRepository;

    private Developer targetDeveloper;
    private Client targetClient;

    private User clientUserA;
    private User clientUserB;
    private User developerUserA;
    private User developerUserB;

    private Project projectForClientA;
    private Project projectForClientB;
    private Project projectForDevA;
    private Project projectForDevB;

    @BeforeEach
    void given_공통_테스트_데이터_세팅() {

        // Given : 공통 카테고리
        Category category = categoryRepository.save(Category.builder()
                .admin(null)
                .name("테스트카테고리")
                .icon("icon")
                .description("테스트용")
                .displayOrder(1)
                .build());

        // ──────────────────────────────────────────────────────
        // Given [시나리오 1] : 2명의 클라이언트 → 1명의 개발자
        // ──────────────────────────────────────────────────────

        // Given : 평점을 받을 개발자
        User developerUser = userRepository.save(User.builder()
                .email("target-dev@test.com").password("pw").name("타겟개발자")
                .phoneNumber("010-0000-0001")
                .userRole(UserRole.DEVELOPER).build());

        targetDeveloper = developerRepository.save(Developer.builder()
                .user(developerUser).title("백엔드 개발자")
                .rating(0.0).reviewCount(0).completedProjects(0)
                .availableForWork(true).participateType(ParticipateType.INDIVIDUAL)
                .skills(List.of("Java")).build());

        // Given : 리뷰를 작성할 클라이언트 A
        clientUserA = userRepository.save(User.builder()
                .email("clientA@test.com").password("pw").name("클라이언트A")
                .phoneNumber("010-0000-0002")
                .userRole(UserRole.CLIENT).build());

        Client clientA = clientRepository.save(Client.builder()
                .user(clientUserA).title("클라이언트A")
                .completedProject(0).rating(0.0).reviewCount(0)
                .participateType(ParticipateType.INDIVIDUAL).build());

        // Given : 리뷰를 작성할 클라이언트 B
        clientUserB = userRepository.save(User.builder()
                .email("clientB@test.com").password("pw").name("클라이언트B")
                .phoneNumber("010-0000-0003")
                .userRole(UserRole.CLIENT).build());

        Client clientB = clientRepository.save(Client.builder()
                .user(clientUserB).title("클라이언트B")
                .completedProject(0).rating(0.0).reviewCount(0)
                .participateType(ParticipateType.INDIVIDUAL).build());

        // Given : 프로젝트1 (clientA 소유, targetDeveloper ACCEPTED 참여)
        projectForClientA = projectRepository.save(Project.builder()
                .client(clientA).title("프로젝트1").description("설명1")
                .category(category).minBudget(1000L).maxBudget(5000L)
                .duration(30).maxProposalCount(5).skills(List.of("Java"))
                .build());
        projectForClientA.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(projectForClientA);

        proposalRepository.save(Proposal.builder()
                .project(projectForClientA).developer(targetDeveloper)
                .coverLetter("테스트 지원서").proposedBudget("1000").proposedDuration("30일")
                .status(ProposalStatus.ACCEPTED).build());

        // Given : 프로젝트2 (clientB 소유, targetDeveloper ACCEPTED 참여)
        projectForClientB = projectRepository.save(Project.builder()
                .client(clientB).title("프로젝트2").description("설명2")
                .category(category).minBudget(1000L).maxBudget(5000L)
                .duration(30).maxProposalCount(5).skills(List.of("Java"))
                .build());
        projectForClientB.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(projectForClientB);

        proposalRepository.save(Proposal.builder()
                .project(projectForClientB).developer(targetDeveloper)
                .coverLetter("테스트 지원서").proposedBudget("1000").proposedDuration("30일")
                .status(ProposalStatus.ACCEPTED).build());

        // ──────────────────────────────────────────────────────
        // Given [시나리오 2] : 2명의 개발자 → 1명의 클라이언트
        // ──────────────────────────────────────────────────────

        // Given : 평점을 받을 클라이언트
        User clientUserForTarget = userRepository.save(User.builder()
                .email("target-client@test.com").password("pw").name("타겟클라이언트")
                .phoneNumber("010-0000-0004")
                .userRole(UserRole.CLIENT).build());

        targetClient = clientRepository.save(Client.builder()
                .user(clientUserForTarget).title("타겟클라이언트")
                .completedProject(0).rating(0.0).reviewCount(0)
                .participateType(ParticipateType.INDIVIDUAL).build());

        // Given : 리뷰를 작성할 개발자 A
        developerUserA = userRepository.save(User.builder()
                .email("devA@test.com").password("pw").name("개발자A")
                .phoneNumber("010-0000-0005")
                .userRole(UserRole.DEVELOPER).build());

        Developer devA = developerRepository.save(Developer.builder()
                .user(developerUserA).title("프론트 개발자")
                .rating(0.0).reviewCount(0).completedProjects(0)
                .availableForWork(true).participateType(ParticipateType.INDIVIDUAL)
                .skills(List.of("React")).build());

        // Given : 리뷰를 작성할 개발자 B
        developerUserB = userRepository.save(User.builder()
                .email("devB@test.com").password("pw").name("개발자B")
                .phoneNumber("010-0000-0006")
                .userRole(UserRole.DEVELOPER).build());

        Developer devB = developerRepository.save(Developer.builder()
                .user(developerUserB).title("백엔드 개발자")
                .rating(0.0).reviewCount(0).completedProjects(0)
                .availableForWork(true).participateType(ParticipateType.INDIVIDUAL)
                .skills(List.of("Spring")).build());

        // Given : 프로젝트3 (targetClient 소유, devA ACCEPTED 참여)
        projectForDevA = projectRepository.save(Project.builder()
                .client(targetClient).title("프로젝트3").description("설명3")
                .category(category).minBudget(1000L).maxBudget(5000L)
                .duration(30).maxProposalCount(5).skills(List.of("React"))
                .build());
        projectForDevA.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(projectForDevA);

        proposalRepository.save(Proposal.builder()
                .project(projectForDevA).developer(devA)
                .coverLetter("테스트 지원서").proposedBudget("1000").proposedDuration("30일")
                .status(ProposalStatus.ACCEPTED).build());

        // Given : 프로젝트4 (targetClient 소유, devB ACCEPTED 참여)
        projectForDevB = projectRepository.save(Project.builder()
                .client(targetClient).title("프로젝트4").description("설명4")
                .category(category).minBudget(1000L).maxBudget(5000L)
                .duration(30).maxProposalCount(5).skills(List.of("Spring"))
                .build());
        projectForDevB.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(projectForDevB);

        proposalRepository.save(Proposal.builder()
                .project(projectForDevB).developer(devB)
                .coverLetter("테스트 지원서").proposedBudget("1000").proposedDuration("30일")
                .status(ProposalStatus.ACCEPTED).build());
    }

    // ══════════════════════════════════════════════════════════════
    // [시나리오 1] 클라이언트 2명이 동시에 개발자 1명에게 평점
    // ══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("[성공] 낙관적 락 적용 후 - 클라이언트 2명이 동시에 개발자 1명에게 리뷰 작성 → Lost Update 없이 정확히 반영")
    void 클라이언트2명_동시_개발자평점_낙관적락_적용후_정합성_보장() throws InterruptedException {

        // Given : 동시 출발을 보장하는 CyclicBarrier
        int threadCount = 2;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Given : clientA → targetDeveloper rating 4점
        ReviewRequestDto requestA = new ReviewRequestDto(projectForClientA.getId(), 4, "좋아요");
        // Given : clientB → targetDeveloper rating 2점
        ReviewRequestDto requestB = new ReviewRequestDto(projectForClientB.getId(), 2, "별로에요");

        // When : 두 스레드 동시 출발 → createReviewWithRatingUpdate 동시 호출
        executorService.submit(() -> {
            try {
                barrier.await();
                reviewTransactionService.createReviewWithRatingUpdate(requestA, targetDeveloper.getUser().getId(), clientUserA.getEmail());
            } catch (Exception e) {
                System.out.println("[시나리오1 - 스레드A 예외] " + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        });

        executorService.submit(() -> {
            try {
                barrier.await();
                reviewTransactionService.createReviewWithRatingUpdate(requestB, targetDeveloper.getUser().getId(), clientUserB.getEmail());
            } catch (Exception e) {
                System.out.println("[시나리오1 - 스레드B 예외] " + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        });

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Then : 낙관적 락 + @Retryable 적용 후 rating, reviewCount 가 정확히 반영되어야 한다
        Developer result = developerRepository.findById(targetDeveloper.getId()).orElseThrow();

        double expectedRating = 3.0;    // (4 + 2) / 2
        int expectedReviewCount = 2;

        System.out.println("\n=== [시나리오1] 클라이언트2명 → 개발자1명 결과 ===");
        System.out.println("실제 rating     : " + result.getRating()      + " (기대: " + expectedRating + ")");
        System.out.println("실제 reviewCount: " + result.getReviewCount() + " (기대: " + expectedReviewCount + ")");
        System.out.println("DB 저장된 리뷰 수: " + reviewRepository.findByDeveloperId(targetDeveloper.getId()).size());
        System.out.println("→ reviewCount 가 2 이면 Lost Update 방어 성공!");

        // 낙관적 락 적용 후 → Lost Update 없이 정확한 값이 반영되어야 성공
        assertThat(result.getReviewCount())
                .as("낙관적 락 적용 후 reviewCount 가 정확히 2로 저장되어야 함")
                .isEqualTo(expectedReviewCount);
        assertThat(result.getRating())
                .as("낙관적 락 적용 후 rating 이 정확히 3.0으로 저장되어야 함")
                .isEqualTo(expectedRating);
    }

    // ══════════════════════════════════════════════════════════════
    // [시나리오 2] 개발자 2명이 동시에 클라이언트 1명에게 평점
    // ══════════════════════════════════════════════════════════════
    @Test
    @DisplayName("[성공] 낙관적 락 적용 후 - 개발자 2명이 동시에 클라이언트 1명에게 리뷰 작성 → Lost Update 없이 정확히 반영")
    void 개발자2명_동시_클라이언트평점_낙관적락_적용후_정합성_보장() throws InterruptedException {

        // Given : 동시 출발을 보장하는 CyclicBarrier
        int threadCount = 2;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Given : devA → targetClient rating 5점
        ReviewRequestDto requestA = new ReviewRequestDto(projectForDevA.getId(), 5, "좋은 클라이언트");
        // Given : devB → targetClient rating 3점
        ReviewRequestDto requestB = new ReviewRequestDto(projectForDevB.getId(), 3, "보통 클라이언트");

        // When : 두 스레드 동시 출발 → createReviewWithRatingUpdate 동시 호출
        executorService.submit(() -> {
            try {
                barrier.await();
                reviewTransactionService.createReviewWithRatingUpdate(requestA, targetClient.getUser().getId(), developerUserA.getEmail());
            } catch (Exception e) {
                System.out.println("[시나리오2 - 스레드A 예외] " + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        });

        executorService.submit(() -> {
            try {
                barrier.await();
                reviewTransactionService.createReviewWithRatingUpdate(requestB, targetClient.getUser().getId(), developerUserB.getEmail());
            } catch (Exception e) {
                System.out.println("[시나리오2 - 스레드B 예외] " + e.getClass().getSimpleName() + " : " + e.getMessage());
            }
        });

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        // Then :  낙관적 락 + @Retryable 적용 후 rating, reviewCount 가 정확히 반영되어야 한다
        Client result = clientRepository.findById(targetClient.getId()).orElseThrow();

        double expectedRating = 4.0;    // (5 + 3) / 2
        int expectedReviewCount = 2;

        System.out.println("\n=== [시나리오2] 낙관적 락 적용 후 - 개발자2명 → 클라이언트1명 결과 ===");
        System.out.println("실제 rating     : " + result.getRating()      + " (기대: " + expectedRating + ")");
        System.out.println("실제 reviewCount: " + result.getReviewCount() + " (기대: " + expectedReviewCount + ")");
        System.out.println("DB 저장된 리뷰 수: " + reviewRepository.findByClientId(targetClient.getId()).size());
        System.out.println("→ reviewCount 가 1 이면 Lost Update 발생 확인!");

        // 낙관적 락 적용 후 → Lost Update 없이 정확한 값이 반영되어야 성공
        assertThat(result.getReviewCount())
                .as("낙관적 락 적용 후 reviewCount 가 정확히 2로 저장되어야 함")
                .isEqualTo(expectedReviewCount);
        assertThat(result.getRating())
                .as("낙관적 락 적용 후 rating 이 정확히 4.0으로 저장되어야 함")
                .isEqualTo(expectedRating);
    }
}