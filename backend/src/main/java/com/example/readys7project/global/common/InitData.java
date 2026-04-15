package com.example.readys7project.global.common;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.domain.review.entity.Review;
import com.example.readys7project.domain.review.repository.ReviewRepository;
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
import java.util.Optional;

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
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final SkillRepository skillRepository;

    private static final String DEFAULT_PASSWORD = "12345678";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initSuperAdmin();
        initSkills();
        initClients();
        initDevelopers();
        initCategories();
        initProjects();
        initProposals();
        initReviews();
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
                    .rating(4.7)
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
                    .rating(4.3)
                    .reviewCount(3)
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
                    .rating(4.5)
                    .reviewCount(2)
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
                    .rating(4.5)
                    .reviewCount(2)
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
                    .rating(4.5)
                    .reviewCount(2)
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

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("모바일")
                    .icon("📱")
                    .description("iOS, Android, Flutter 등 모바일 앱 개발")
                    .displayOrder(4)
                    .build());

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("DevOps")
                    .icon("⚙️")
                    .description("CI/CD, Docker, Kubernetes, 클라우드 인프라")
                    .displayOrder(5)
                    .build());

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("데이터베이스")
                    .icon("🗄️")
                    .description("MySQL, PostgreSQL, MongoDB 등 DB 설계 및 최적화")
                    .displayOrder(6)
                    .build());

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("보안")
                    .icon("🔒")
                    .description("보안 취약점 분석, 침투 테스트, 보안 솔루션 개발")
                    .displayOrder(7)
                    .build());

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("UI/UX")
                    .icon("✏️")
                    .description("사용자 인터페이스 및 경험 설계")
                    .displayOrder(8)
                    .build());

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("블록체인")
                    .icon("🔗")
                    .description("스마트 컨트랙트, NFT, DeFi 개발")
                    .displayOrder(9)
                    .build());

            categoryRepository.save(Category.builder()
                    .admin(superAdmin)
                    .name("게임")
                    .icon("🎮")
                    .description("Unity, Unreal 등 게임 클라이언트/서버 개발")
                    .displayOrder(10)
                    .build());

            log.info("[InitData] Category 10개 생성 완료");
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

        /**
         *  리뷰용 완료프로젝트 목록
         */
        Project reviewProject1 = Project.builder()
                .client(client1)
                .category(backendCategory)
                .title("백엔드 API 유지보수 및 개선")
                .description("기존 Spring Boot 기반 API 서버의 성능 개선과 유지보수를 진행하는 프로젝트입니다. "
                        + "인증, 주문, 결제 모듈 안정화 작업이 포함됩니다.")
                .skills(List.of("Java", "Spring Boot", "JPA"))
                .minBudget(500000)
                .maxBudget(1000000)
                .duration(30)
                .maxProposalCount(1)
                .build();
        reviewProject1.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(reviewProject1);

        Project reviewProject2 = Project.builder()
                .client(client1)
                .category(frontendCategory)
                .title("관리자 페이지 UI 개선")
                .description("React + TypeScript 기반 관리자 페이지의 화면 구조 개선과 공통 컴포넌트 정리를 진행하는 프로젝트입니다.")
                .skills(List.of("React", "TypeScript", "Tailwind CSS"))
                .minBudget(500000)
                .maxBudget(1000000)
                .duration(30)
                .maxProposalCount(1)
                .build();
        reviewProject2.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(reviewProject2);

        Project reviewProject3 = Project.builder()
        // maxProposalCount를 1로 설정하면
        // increaseProposalCount() 1번 호출만으로 CLOSED 됨
        Project closedProject = projectRepository.save(Project.builder()
                .client(client1)
                .category(aiCategory)
                .title("추천 모델 성능 개선")
                .description("기존 추천 시스템의 정확도 개선을 위해 모델 튜닝과 간단한 API 연동을 진행하는 프로젝트입니다.")
                .skills(List.of("Python", "TensorFlow", "FastAPI"))
                .minBudget(500000)
                .maxBudget(1000000)
                .duration(30)
                .maxProposalCount(1)
                .build();
        reviewProject3.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(reviewProject3);

        Project reviewProject4 = Project.builder()
                .client(client2)
                .category(backendCategory)
                .title("주문 관리 서버 기능 추가")
                .description("주문 상태 변경, 결제 이력 조회 등 백엔드 기능을 추가하는 프로젝트입니다.")
                .skills(List.of("Java", "Spring Boot", "MySQL"))
                .minBudget(500000)
                .maxBudget(1000000)
                .duration(30)
                .maxProposalCount(1)
                .build();
        reviewProject4.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(reviewProject4);

        Project reviewProject5 = Project.builder()
                .client(client2)
                .category(frontendCategory)
                .title("대시보드 프론트엔드 고도화")
                .description("기존 대시보드의 차트와 테이블 UI를 개선하고 반응형 화면을 보강하는 프로젝트입니다.")
                .skills(List.of("React", "TypeScript", "Chart.js"))
                .minBudget(500000)
                .maxBudget(1000000)
                .duration(30)
                .maxProposalCount(1)
                .build();
        reviewProject5.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(reviewProject5);

        Project reviewProject6 = Project.builder()
                .client(client2)
                .category(aiCategory)
                .title("AI 분석 기능 시범 적용")
                .description("사용자 행동 데이터를 기반으로 간단한 분석 기능과 예측 모델을 시범 적용하는 프로젝트입니다.")
                .skills(List.of("Python", "TensorFlow", "Docker"))
                .minBudget(500000)
                .maxBudget(1000000)
                .duration(30)
                .maxProposalCount(1)
                .build();
        reviewProject6.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(reviewProject6);

        log.info("[InitData] Project 9개 생성 완료");
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
        Project project4 = projects.get(3);// 완료된 제안서
        Project project5 = projects.get(4);
        Project project6 = projects.get(5);
        Project project7 = projects.get(6);
        Project project8 = projects.get(7);
        Project project9 = projects.get(8);


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
        project2.changeStatus(ProjectStatus.IN_PROGRESS);
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


        /**
         *  완료 리뷰용 제안서.
         */
        proposalRepository.save(Proposal.builder()
                .project(project4)
                .developer(dev1)
                .coverLetter("안녕하세요. 5년차 백엔드 개발자 박백엔드입니다. "
                        + "Spring Boot와 JPA 기반 API 서버 개발 경험이 풍부하며, "
                        + "client1과의 리뷰 테스트 프로젝트를 안정적으로 완료할 수 있습니다.")
                .proposedBudget("900000")
                .proposedDuration("25")
                .status(ProposalStatus.ACCEPTED)
                .build());
        project4.increaseProposalCount();
        project4.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project4);

        proposalRepository.save(Proposal.builder()
                .project(project5)
                .developer(dev2)
                .coverLetter("안녕하세요. 풀스택 개발자 최풀스택입니다. "
                        + "React와 TypeScript 기반 화면 개발 경험이 풍부하며, "
                        + "client1과의 리뷰 테스트 프로젝트를 원활하게 수행할 수 있습니다.")
                .proposedBudget("900000")
                .proposedDuration("25")
                .status(ProposalStatus.ACCEPTED)
                .build());
        project5.increaseProposalCount();
        project5.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project5);

        proposalRepository.save(Proposal.builder()
                .project(project6)
                .developer(dev3)
                .coverLetter("안녕하세요. AI/ML 개발자 정에이아이입니다. "
                        + "Python과 TensorFlow 기반 모델 개발 경험이 있으며, "
                        + "client1과의 리뷰 테스트 프로젝트도 책임감 있게 완료하겠습니다.")
                .proposedBudget("900000")
                .proposedDuration("25")
                .status(ProposalStatus.ACCEPTED)
                .build());
        project6.increaseProposalCount();
        project6.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project6);

        proposalRepository.save(Proposal.builder()
                .project(project7)
                .developer(dev1)
                .coverLetter("안녕하세요. 백엔드 개발자 박백엔드입니다. "
                        + "REST API 설계와 서버 개발 경험을 바탕으로 "
                        + "client2와의 리뷰 테스트 프로젝트를 안정적으로 진행하겠습니다.")
                .proposedBudget("900000")
                .proposedDuration("25")
                .status(ProposalStatus.ACCEPTED)
                .build());
        project7.increaseProposalCount();
        project7.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project7);

        proposalRepository.save(Proposal.builder()
                .project(project8)
                .developer(dev2)
                .coverLetter("안녕하세요. 풀스택 개발자 최풀스택입니다. "
                        + "프론트엔드와 백엔드를 모두 아우르는 경험을 바탕으로 "
                        + "client2와의 리뷰 테스트 프로젝트를 성실히 수행하겠습니다.")
                .proposedBudget("900000")
                .proposedDuration("25")
                .status(ProposalStatus.ACCEPTED)
                .build());
        project8.increaseProposalCount();
        project8.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project8);

        proposalRepository.save(Proposal.builder()
                .project(project9)
                .developer(dev3)
                .coverLetter("안녕하세요. AI/ML 개발자 정에이아이입니다. "
                        + "데이터 기반 문제 해결 경험을 바탕으로 "
                        + "client2와의 리뷰 테스트 프로젝트를 만족스럽게 마무리하겠습니다.")
                .proposedBudget("900000")
                .proposedDuration("25")
                .status(ProposalStatus.ACCEPTED)
                .build());
        project9.increaseProposalCount();
        project9.changeStatus(ProjectStatus.COMPLETED);
        projectRepository.save(project9);





        log.info("[InitData] Proposal 3개 생성 완료 (PENDING / ACCEPTED / WITHDRAWN)");
        log.info("[InitData] Proposal 완료 상태 6개 생성 완료");
        log.info("[InitData] ===== 전체 이닛데이터 생성 완료 =====");
        log.info("[InitData] 채팅방 생성 테스트: client1@test.com 로그인 → project2(관리자 대시보드)에서 dev2와 채팅방 생성 가능");
    }

    private void initSkills() {

        Admin superAdmin = adminRepository.findByUserEmail("superAdmin@system.com")
                .orElseThrow(() -> new IllegalStateException("[InitData] SuperAdmin이 없습니다."));

        saveSkillIfAbsent(superAdmin, SkillCategory.BACKEND, List.of(
                "Java", "Spring Boot", "MySQL", "Redis", "Python",
                "Kotlin", "Scala", "Oracle", "C++", "C#", "C", "Node.js", "Go"));
        saveSkillIfAbsent(superAdmin, SkillCategory.FRONTEND, List.of(
                "React", "TypeScript", "JavaScript", "Vue.js",
                "Swift", "Next.js", "Figma", "HTML", "CSS", "Blender",
                "Autodesk Maya", "3ds Max", "ZBrush", "Designer", "Substance Painter"));
        saveSkillIfAbsent(superAdmin, SkillCategory.INFRA, List.of(
                "AWS", "Docker", "GitHub", "Jira", "Slack", "Discord"));
        saveSkillIfAbsent(superAdmin, SkillCategory.AI, List.of("AI"));
        saveSkillIfAbsent(superAdmin, SkillCategory.GAME, List.of(
                "Unreal", "Unity", "Godot", "RPG Maker", "GameMaker", "Construct 3"));

    }

    private void saveSkillIfAbsent(Admin admin, SkillCategory category, List<String> skillNames) {

        if (skillNames == null || skillNames.isEmpty()) { return; }

        // DB에 이미 존재하는 기술들의 이름만 한번의 쿼리로 가져옴
        List<String> existingNames = skillRepository.findAllByNameIn(skillNames)
                .stream()
                .map(Skill::getName)
                .toList();

        // 입력 받은 리스트 중 DB에 없는 이름만 골라내어 Entity로 변환
        List<Skill> newSkills = skillNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> Skill.builder()
                        .admin(admin)
                        .name(name)
                        .skillCategory(category)
                        .build())
                .toList();

        // 새로운 데이터가 있을 때만 saveAll 호출
        if (!newSkills.isEmpty()) {
            skillRepository.saveAll(newSkills);
        }
    /**
     * 리뷰 더미 데이터
     * 리뷰 갯수는 6개가 자동으로 생성 되도록 설정.
     */
    private void initReviews() {
        if (reviewRepository.count() > 0) {
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
        Project project4 = projects.get(3);
        Project project5 = projects.get(4);
        Project project6 = projects.get(5);
        Project project7 = projects.get(6);
        Project project8 = projects.get(7);
        Project project9 = projects.get(8);

        reviewRepository.save(Review.builder()
                .client(client1)
                .developer(dev1)
                .project(project4)
                .rating(5)
                .comment("클라이언트 : 클라이언트가 남긴 초기 리뷰입니다.")
                .build());

        reviewRepository.save(Review.builder()
                .client(client1)
                .developer(dev1)
                .project(project4)
                .rating(4)
                .comment("개발자 : 일정 조율과 소통이 좋았습니다.")
                .build());

        reviewRepository.save(Review.builder()
                .client(client1)
                .developer(dev2)
                .project(project5)
                .rating(5)
                .comment("클라이언트 : 대시보드 구현 완성도가 높았습니다.")
                .build());

        reviewRepository.save(Review.builder()
                .client(client2)
                .developer(dev2)
                .project(project8)
                .rating(4)
                .comment("개발자 : 응답이 빠르고 협업이 원활했습니다.")
                .build());

        reviewRepository.save(Review.builder()
                .client(client2)
                .developer(dev3)
                .project(project9)
                .rating(5)
                .comment("클라이언트 : AI 모델 성능이 기대 이상이었습니다.")
                .build());

        reviewRepository.save(Review.builder()
                .client(client2)
                .developer(dev3)
                .project(project9)
                .rating(4)
                .comment("개발자 : 설명이 명확했고 결과물도 만족스러웠습니다.")
                .build());
        log.info("[InitData] Review 6개 생성 완료");
    }
}
