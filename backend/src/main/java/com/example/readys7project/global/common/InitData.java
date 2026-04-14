package com.example.readys7project.global.common;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.enums.ParticipateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitData implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final DeveloperRepository developerRepository;
    private final CategoryRepository categoryRepository;
    private final ProjectRepository projectRepository;
    private final ProposalRepository proposalRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "12345678";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initSuperAdmin();
        initClients();
        initDevelopers();
        initCategories();
        initProjects();
        initProposals();
    }

    // ─────────────────────────────────────────────
    // 1. Super Admin
    // ─────────────────────────────────────────────
    private void initSuperAdmin() {
        if (adminRepository.findByUserEmail("superAdmin@system.com").isPresent()) {
            return;
        }

        User user = userRepository.save(User.builder()
                .email("superAdmin@system.com")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .name("슈퍼 어드민")
                .userRole(UserRole.ADMIN)
                .phoneNumber("01012121212")
                .description("시스템 최고 관리자입니다.")
                .build());

        adminRepository.save(Admin.createSuperAdmin(user));
        log.info("[InitData] Super Admin 생성 완료");
    }

    // ─────────────────────────────────────────────
    // 2. Clients (2명)
    // ─────────────────────────────────────────────
    private void initClients() {
        // Client 1 - 기업형 클라이언트
        if (userRepository.findByEmail("client1@test.com").isEmpty()) {
            User user = userRepository.save(User.builder()
                    .email("client1@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .name("김기업")
                    .userRole(UserRole.CLIENT)
                    .phoneNumber("01011112222")
                    .description("IT 스타트업 CTO입니다. 좋은 개발자를 찾고 있습니다.")
                    .build());

            clientRepository.save(Client.builder()
                    .user(user)
                    .title("스타트업 CTO")
                    .rating(4.5)
                    .reviewCount(3)
                    .completedProject(3)
                    .participateType(ParticipateType.COMPANY)
                    .build());

            log.info("[InitData] Client 1 (김기업) 생성 완료");
        }

        // Client 2 - 개인 클라이언트
        if (userRepository.findByEmail("client2@test.com").isEmpty()) {
            User user = userRepository.save(User.builder()
                    .email("client2@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .name("이개인")
                    .userRole(UserRole.CLIENT)
                    .phoneNumber("01033334444")
                    .description("개인 사업자로 쇼핑몰 개발이 필요합니다.")
                    .build());

            clientRepository.save(Client.builder()
                    .user(user)
                    .title("개인 사업자")
                    .rating(4.0)
                    .reviewCount(1)
                    .completedProject(1)
                    .participateType(ParticipateType.INDIVIDUAL)
                    .build());

            log.info("[InitData] Client 2 (이개인) 생성 완료");
        }
    }

    // ─────────────────────────────────────────────
    // 3. Developers (3명)
    // ─────────────────────────────────────────────
    private void initDevelopers() {
        // Developer 1 - 백엔드 시니어
        if (userRepository.findByEmail("dev1@test.com").isEmpty()) {
            User user = userRepository.save(User.builder()
                    .email("dev1@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .name("박백엔드")
                    .userRole(UserRole.DEVELOPER)
                    .phoneNumber("01055556666")
                    .description("Spring Boot, JPA 전문 백엔드 개발자입니다. 5년 경력.")
                    .build());

            developerRepository.save(Developer.builder()
                    .user(user)
                    .title("시니어 백엔드 개발자")
                    .rating(4.8)
                    .reviewCount(10)
                    .completedProjects(8)
                    .minHourlyPay(50000)
                    .maxHourlyPay(80000)
                    .skills(List.of("Java", "Spring Boot", "JPA", "MySQL", "Redis", "Docker"))
                    .responseTime("2시간 이내")
                    .availableForWork(true)
                    .participateType(ParticipateType.INDIVIDUAL)
                    .build());

            log.info("[InitData] Developer 1 (박백엔드) 생성 완료");
        }

        // Developer 2 - 풀스택
        if (userRepository.findByEmail("dev2@test.com").isEmpty()) {
            User user = userRepository.save(User.builder()
                    .email("dev2@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .name("최풀스택")
                    .userRole(UserRole.DEVELOPER)
                    .phoneNumber("01077778888")
                    .description("React + Node.js 풀스택 개발자입니다. 3년 경력.")
                    .build());

            developerRepository.save(Developer.builder()
                    .user(user)
                    .title("풀스택 개발자")
                    .rating(4.3)
                    .reviewCount(5)
                    .completedProjects(4)
                    .minHourlyPay(35000)
                    .maxHourlyPay(60000)
                    .skills(List.of("React", "TypeScript", "Node.js", "Spring Boot", "AWS"))
                    .responseTime("4시간 이내")
                    .availableForWork(true)
                    .participateType(ParticipateType.INDIVIDUAL)
                    .build());

            log.info("[InitData] Developer 2 (최풀스택) 생성 완료");
        }

        // Developer 3 - AI/ML 전문
        if (userRepository.findByEmail("dev3@test.com").isEmpty()) {
            User user = userRepository.save(User.builder()
                    .email("dev3@test.com")
                    .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                    .name("정에이아이")
                    .userRole(UserRole.DEVELOPER)
                    .phoneNumber("01099990000")
                    .description("Python, ML 모델 개발 전문가입니다. 4년 경력.")
                    .build());

            developerRepository.save(Developer.builder()
                    .user(user)
                    .title("AI/ML 개발자")
                    .rating(4.6)
                    .reviewCount(7)
                    .completedProjects(6)
                    .minHourlyPay(60000)
                    .maxHourlyPay(100000)
                    .skills(List.of("Python", "TensorFlow", "PyTorch", "FastAPI", "Docker", "Kubernetes"))
                    .responseTime("1시간 이내")
                    .availableForWork(false)
                    .participateType(ParticipateType.COMPANY)
                    .build());

            log.info("[InitData] Developer 3 (정에이아이) 생성 완료");
        }
    }

    // ─────────────────────────────────────────────
    // 4. Categories (3개)
    // Admin이 FK로 필요 → superAdmin 조회 후 사용
    // ─────────────────────────────────────────────
    private void initCategories() {
        if (!categoryRepository.existsByName("백엔드")) {
            Admin superAdmin = adminRepository.findByUserEmail("superAdmin@system.com")
                    .orElseThrow(() -> new IllegalStateException("[InitData] SuperAdmin이 존재하지 않습니다."));

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("백엔드")
                    .icon("🖥️")
                    .description("Java, Spring, Node.js 등 서버사이드 개발")
                    .displayOrder(1)
                    .build());

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("프론트엔드")
                    .icon("🎨")
                    .description("React, Vue, Angular 등 클라이언트사이드 개발")
                    .displayOrder(2)
                    .build());

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("AI/ML")
                    .icon("🤖")
                    .description("머신러닝, 딥러닝, 데이터 분석 프로젝트")
                    .displayOrder(3)
                    .build());

            log.info("[InitData] Category 3개 생성 완료");
        }
    }

    // ─────────────────────────────────────────────
    // 5. Projects (3개)
    // Client + Category가 먼저 존재해야 생성 가능
    // ─────────────────────────────────────────────
    private void initProjects() {
        if (projectRepository.count() > 0) {
            return;
        }

        Client client1 = clientRepository.findByUser(
                userRepository.findByEmail("client1@test.com")
                        .orElseThrow(() -> new IllegalStateException("[InitData] client1이 존재하지 않습니다."))
        ).orElseThrow(() -> new IllegalStateException("[InitData] client1 엔티티가 존재하지 않습니다."));

        Client client2 = clientRepository.findByUser(
                userRepository.findByEmail("client2@test.com")
                        .orElseThrow(() -> new IllegalStateException("[InitData] client2가 존재하지 않습니다."))
        ).orElseThrow(() -> new IllegalStateException("[InitData] client2 엔티티가 존재하지 않습니다."));

        Category backendCategory = categoryRepository.findByName("백엔드")
                .orElseThrow(() -> new IllegalStateException("[InitData] 백엔드 카테고리가 존재하지 않습니다."));

        Category frontendCategory = categoryRepository.findByName("프론트엔드")
                .orElseThrow(() -> new IllegalStateException("[InitData] 프론트엔드 카테고리가 존재하지 않습니다."));

        Category aiCategory = categoryRepository.findByName("AI/ML")
                .orElseThrow(() -> new IllegalStateException("[InitData] AI/ML 카테고리가 존재하지 않습니다."));

        // Project 1 - 백엔드 API 개발 (OPEN, 제안서 접수 중)
        projectRepository.save(Project.builder()
                .client(client1)
                .category(backendCategory)
                .title("쇼핑몰 백엔드 API 개발")
                .description("Spring Boot 기반 쇼핑몰 REST API 개발 프로젝트입니다. "
                        + "회원, 상품, 주문, 결제 기능이 필요합니다.")
                .skills(List.of("Java", "Spring Boot", "JPA", "MySQL", "Redis"))
                .minBudget(3000000)
                .maxBudget(5000000)
                .duration(60)
                .maxProposalCount(5)
                .build());

        // Project 2 - 관리자 대시보드 (OPEN, 채팅 테스트용 → 제안서 ACCEPTED 포함)
        projectRepository.save(Project.builder()
                .client(client1)
                .category(frontendCategory)
                .title("관리자 대시보드 UI 개발")
                .description("React + TypeScript 기반 관리자 대시보드 개발. "
                        + "차트, 테이블, 실시간 알림 기능 포함.")
                .skills(List.of("React", "TypeScript", "Chart.js", "Tailwind CSS"))
                .minBudget(2000000)
                .maxBudget(4000000)
                .duration(45)
                .maxProposalCount(3)
                .build());

        // Project 3 - AI 추천 시스템 (client2 소유)
        projectRepository.save(Project.builder()
                .client(client2)
                .category(aiCategory)
                .title("상품 추천 AI 모델 개발")
                .description("사용자 행동 데이터 기반 상품 추천 시스템 개발. "
                        + "협업 필터링 및 콘텐츠 기반 필터링 혼합 방식.")
                .skills(List.of("Python", "TensorFlow", "FastAPI", "Docker"))
                .minBudget(5000000)
                .maxBudget(10000000)
                .duration(90)
                .maxProposalCount(3)
                .build());

        log.info("[InitData] Project 3개 생성 완료");
    }

    // ─────────────────────────────────────────────
    // 6. Proposals (3개)
    // Project + Developer가 먼저 존재해야 생성 가능
    // PENDING 1개, ACCEPTED 1개, WITHDRAWN 1개
    // ─────────────────────────────────────────────
    private void initProposals() {
        if (proposalRepository.count() > 0) {
            return;
        }

        Developer dev1 = developerRepository.findByUser(
                userRepository.findByEmail("dev1@test.com")
                        .orElseThrow(() -> new IllegalStateException("[InitData] dev1이 존재하지 않습니다."))
        ).orElseThrow(() -> new IllegalStateException("[InitData] dev1 엔티티가 존재하지 않습니다."));

        Developer dev2 = developerRepository.findByUser(
                userRepository.findByEmail("dev2@test.com")
                        .orElseThrow(() -> new IllegalStateException("[InitData] dev2가 존재하지 않습니다."))
        ).orElseThrow(() -> new IllegalStateException("[InitData] dev2 엔티티가 존재하지 않습니다."));

        Developer dev3 = developerRepository.findByUser(
                userRepository.findByEmail("dev3@test.com")
                        .orElseThrow(() -> new IllegalStateException("[InitData] dev3이 존재하지 않습니다."))
        ).orElseThrow(() -> new IllegalStateException("[InitData] dev3 엔티티가 존재하지 않습니다."));

        List<Project> projects = projectRepository.findAll();
        Project project1 = projects.get(0); // 쇼핑몰 백엔드 API
        Project project2 = projects.get(1); // 관리자 대시보드
        Project project3 = projects.get(2); // AI 추천 시스템

        // Proposal 1 - PENDING (project1에 dev1이 지원, 검토 중)
        proposalRepository.save(Proposal.builder()
                .project(project1)
                .developer(dev1)
                .coverLetter("안녕하세요. 5년차 백엔드 개발자 박백엔드입니다. "
                        + "Spring Boot와 JPA를 활용한 쇼핑몰 API 개발 경험이 풍부합니다. "
                        + "제시된 기간 내에 완벽하게 납품할 자신이 있습니다.")
                .proposedBudget("4000000")
                .proposedDuration("55")
                .status(ProposalStatus.PENDING)
                .build());

        // Proposal 2 - ACCEPTED (project2에 dev2가 지원 → 수락됨, 채팅방 생성 테스트 가능)
        // 주의: ACCEPTED 상태이므로 project2의 currentProposalCount를 수동으로 반영해야 함
        // → project.increaseProposalCount() 호출
        Proposal acceptedProposal = proposalRepository.save(Proposal.builder()
                .project(project2)
                .developer(dev2)
                .coverLetter("안녕하세요. 풀스택 개발자 최풀스택입니다. "
                        + "React + TypeScript 기반 대시보드 개발 경험이 있으며, "
                        + "Chart.js를 활용한 데이터 시각화도 자신 있습니다.")
                .proposedBudget("3500000")
                .proposedDuration("40")
                .status(ProposalStatus.ACCEPTED)
                .build());

        // ACCEPTED 상태이므로 프로젝트의 제안서 수 증가 처리
        project2.increaseProposalCount();
        projectRepository.save(project2);

        // Proposal 3 - WITHDRAWN (project3에 dev3이 지원했다가 철회)
        proposalRepository.save(Proposal.builder()
                .project(project3)
                .developer(dev3)
                .coverLetter("AI/ML 전문 개발자 정에이아이입니다. "
                        + "TensorFlow 기반 추천 시스템 개발 경험이 있습니다. "
                        + "하지만 일정이 겹쳐 부득이하게 철회합니다.")
                .proposedBudget("8000000")
                .proposedDuration("80")
                .status(ProposalStatus.WITHDRAWN)
                .build());

        log.info("[InitData] Proposal 3개 생성 완료 (PENDING / ACCEPTED / WITHDRAWN)");
        log.info("[InitData] ===== 전체 이닛데이터 생성 완료 =====");
        log.info("[InitData] 채팅방 생성 테스트: client1@test.com 로그인 → project2(관리자 대시보드)에서 dev2와 채팅방 생성 가능");
    }
}
