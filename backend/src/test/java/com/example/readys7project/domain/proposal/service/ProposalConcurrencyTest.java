package com.example.readys7project.domain.proposal.service;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.enums.ParticipateType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProposalConcurrencyTest {

    @Autowired ProposalService proposalService;
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired DeveloperRepository developerRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProposalRepository proposalRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final int THREAD_COUNT = 100;       // 동시 요청 개발자 수
    private static final int MAX_PROPOSAL_COUNT = 10;  // 제안서 최대 한도

    private Long projectId;
    private List<String> developerEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // ── 카테고리 ──────────────────────────────────────
        Category category = categoryRepository.findByName("백엔드")
                .orElseGet(() -> categoryRepository.save(
                        Category.builder()
                                .name("백엔드")
                                .icon("backend")
                                .description("테스트용 카테고리")
                                .displayOrder(99)
                                .build()
                ));

        // ── CLIENT + 프로젝트 생성 ─────────────────────────
        User clientUser = userRepository.save(User.builder()
                .email("test-client@test.com")
                .password(passwordEncoder.encode("12345678"))
                .name("테스트클라이언트")
                .userRole(UserRole.CLIENT)
                .phoneNumber("01000000000")
                .build());

        Client client = clientRepository.save(Client.builder()
                .user(clientUser)
                .title("테스트 클라이언트")
                .completedProject(0)
                .rating(0.0)
                .reviewCount(0)
                .participateType(ParticipateType.INDIVIDUAL)
                .build());

        // maxProposalCount = 10 → 10개만 제안서 받을 수 있는 프로젝트
        Project project = projectRepository.save(Project.builder()
                .client(client)
                .category(category)
                .title("일주일에 1억짜리 프로젝트")
                .description("동시성 테스트용 프로젝트")
                .skills(List.of("Java", "Spring Boot"))
                .minBudget(100000000L)
                .maxBudget(100000000L)
                .duration(7)
                .maxProposalCount(MAX_PROPOSAL_COUNT)
                .build());

        projectId = project.getId();

        // ── DEVELOPER 100명 생성 ───────────────────────────
        for (int i = 1; i <= THREAD_COUNT; i++) {
            String email = "test-dev-" + i + "@test.com";
            developerEmails.add(email);

            User devUser = userRepository.save(User.builder()
                    .email(email)
                    .password(passwordEncoder.encode("12345678"))
                    .name("테스트개발자" + i)
                    .userRole(UserRole.DEVELOPER)
                    .phoneNumber("010" + String.format("%08d", i))  // (각자 다른 번호)
                    .build());

            developerRepository.save(Developer.builder()
                    .user(devUser)
                    .title("개발자 " + i)
                    .skills(List.of("Java"))
                    .minHourlyPay(30000)
                    .maxHourlyPay(50000)
                    .availableForWork(true)
                    .participateType(ParticipateType.INDIVIDUAL)
                    .completedProjects(0)
                    .rating(0.0)
                    .reviewCount(0)
                    .build());
        }
    }

    @AfterEach
    void tearDown() {
        // 테스트 데이터 정리 (외래키 순서대로!)
        proposalRepository.deleteAll();
        projectRepository.deleteAll();
        developerRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Lock 적용 후 - 100명이 동시에 제안서 제출 시 정합성 보장 (성공해야 정상!)")
    void 동시성_이슈_해결_테스트() throws InterruptedException {

        // ── Given ─────────────────────────────────────────
        // 100명의 개발자가 동시에 제안서를 제출할 준비가 된 상태
        // maxProposalCount = 10 이므로 정확히 10개만 성공해야 함
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // CyclicBarrier: 100명 모두 준비될 때까지 대기 후 동시에 출발시키는 출발선
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);

        // CountDownLatch: 100명이 모두 완료될 때까지 메인 스레드가 대기하기 위한 신호
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // 성공/실패 카운트 (멀티스레드 환경이므로 AtomicInteger 사용)
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // ── When ──────────────────────────────────────────
        // 100명의 개발자가 동시에 제안서 제출 요청
        for (int i = 0; i < THREAD_COUNT; i++) {
            final String email = developerEmails.get(i);

            executor.submit(() -> {
                try {
                    // 100명 모두 이 지점에 도달할 때까지 대기
                    // → 모두 모이면 동시에 출발! (진짜 동시성 재현)
                    barrier.await();

                    ProposalRequestDto request = ProposalRequestDto.builder()
                            .projectId(projectId)
                            .coverLetter("안녕하세요. 제안서입니다.")
                            .proposedBudget("100000000")
                            .proposedDuration("7")
                            .build();

                    proposalService.createProposal(request, email);
                    successCount.incrementAndGet(); // 제출 성공 카운트 증가

                } catch (Exception e) {
                    failCount.incrementAndGet(); // 제출 실패 카운트 증가
                } finally {
                    latch.countDown(); // 완료 신호 (성공/실패 무관하게 호출)
                }
            });
        }

        // 100명 전부 완료될 때까지 메인 스레드 대기
        latch.await();
        executor.shutdown();

        // ── Then ──────────────────────────────────────────
        // DB에서 최종 상태 조회
        Project result = projectRepository.findById(projectId).orElseThrow();

        // 결과 출력
        System.out.println("===== 동시성 테스트 결과 =====");
        System.out.println("✅ 성공 요청 수 : " + successCount.get());
        System.out.println("❌ 실패 요청 수 : " + failCount.get());
        System.out.println("📊 최종 제안서 수 : " + result.getCurrentProposalCount());
        System.out.println("📌 프로젝트 상태 : " + result.getStatus());
        System.out.println("================================");

        // 아래 검증은 Lock이 없으므로 실패해야 정상!
        // 정합성이 지켜졌다면 → successCount = 10, currentProposalCount = 10
        // 정합성이 깨졌다면  → successCount > 10, currentProposalCount > 10 (이게 현재 상황)
        assertThat(successCount.get())
                .as("성공한 제안서 수가 정확히 10개여야 합니다.")
                .isEqualTo(MAX_PROPOSAL_COUNT);

        assertThat(result.getCurrentProposalCount())
                .as("DB의 currentProposalCount가 정확히 10이어야 합니다.")
                .isEqualTo(MAX_PROPOSAL_COUNT);
    }
}