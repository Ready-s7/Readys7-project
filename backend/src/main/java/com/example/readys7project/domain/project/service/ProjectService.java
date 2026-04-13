package com.example.readys7project.domain.project.service;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.dto.ProjectDto;
import com.example.readys7project.domain.project.dto.request.ProjectRequestDto;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectQueryRepositoryImpl;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final CategoryRepository categoryRepository;
    private final ProjectQueryRepositoryImpl projectQueryRepositoryImpl;

    /**
     * 프로젝트 생성 (CLIENT 역할을 가진 사용자만 가능)
     *  프로젝트 생성 시 status = OPEN, currentProposalCount = 0 으로 자동 초기화
     */
    @Transactional
    public ProjectDto createProject(ProjectRequestDto request, String userEmail) {
        // 1. 요청한 사용자 존재 여부 검증
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        // 2. CLIENT 역할 검증
        if (user.getUserRole() != UserRole.CLIENT) {
            throw new ProjectException(ErrorCode.USER_FORBIDDEN);
        }

        // 3. 요청한 사용자의 Client 프로필 존재 여부 검증
        Client client = clientRepository.findByUser(user)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        // 4. 요청한 카테고리 존재 여부 검증
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ProjectException(ErrorCode.CATEGORY_NOT_FOUND));

        // 5. 프로젝트 엔티티 생성 및 저장
        // currentProposalCount = 0, status = OPEN 은 엔티티 내부에서 자동 초기화
        Project project = Project.builder()
                .client(client)
                .category(category)
                .title(request.title())
                .description(request.description())
                .skills(request.skills())
                .minBudget(request.minBudget())
                .maxBudget(request.maxBudget())
                .duration(request.duration())
                .maxProposalCount(request.maxProposalCount())
                .build();

        // 6. 저장 후 DTO로 반환하여 반환
        return convertToDto(projectRepository.save(project));
    }

    /**
     * 전체 프로젝트 목록 조회
     * - 인증 없이 누구나 조회 가능
     * - readOnly = true 로 설정하여 불필요한 스냅샷 생성을 방지하고 성능을 최적화
     */
    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects() {

        // 전체 프로젝트 조회 후 DTO 변환하여 반환
        return projectRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 프로젝트 단건 조회
     * - 인증 없이 누구나 조회 가능
     */
    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {

        // 1. 프로젝트 존재 여부 검증
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        // 2. DTO로 변환하여 반환
        return convertToDto(project);
    }

    /**
     * 프로젝트 검색
     * - category, status, skill 조건으로 필터링
     * - 각 조건은 null일 경우 무시 (동적 쿼리)
     * - 인증 없이 누구나 조회 가능
     */
    @Transactional(readOnly = true)
    public Page<ProjectDto> searchProjects(Long categoryId, String status, List<String> skill, Pageable pageable) {

        // 1. category 조회 (null이면 전체 조회)
        Category category = categoryId != null
                ? categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProjectException(ErrorCode.CATEGORY_NOT_FOUND))
                : null;

        // 2. status 문자열을 Enum으로 변환 (null이면 전체 조회)
        ProjectStatus projectStatus = status != null ?
            ProjectStatus.valueOf(status.toUpperCase()) : null;

        // 3. 조건에 맞는 프로젝트 목록 조회 후 DTO 변환하여 반환
        return projectQueryRepositoryImpl.searchProjects(category, projectStatus, skill, pageable)
                .map(this::convertToDto);
    }

    /**
     * 프로젝트 수정 (본인 CLIENT만 가능)
     * - @Transactional 덕분에 별도 save() 호출 없이 변경사항이 자동 반영 (Dirty Checking)
     */
    @Transactional
    public ProjectDto updateProject(Long id, ProjectRequestDto request, String userEmail) {

        // 1. 수정할 프로젝트 존재 여부 검증
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        // 2. 요청한 사용자 존재 여부 검증
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        // 3. 요청한 사용자의 Client 역할 검증
        // CLIENT 역할이 아닌 경우 차단
        Client client = clientRepository.findByUser(user)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        // 4. 프로젝트 소유자 본인 여부 검증
        // project.getClient().getId() = 프로젝트를 등록한 Client의 id
        // client.getId() = 현재 요청한 사용자의 Client id
        if (!project.getClient().getId().equals(client.getId())) {
            throw new ProjectException(ErrorCode.USER_FORBIDDEN);
        }

        // 5. 변경할 카테고리 존재 여부 검증
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ProjectException(ErrorCode.CATEGORY_NOT_FOUND));

        // 6. 프로젝트 정보 업데이트
        // Dirty Checking에 의해 트랜잭션 종료 시 자동으로 UPDATE 쿼리가 실행됨
        project.updateProject(
                request.title(),
                request.description(),
                category,
                request.skills(),
                request.minBudget(),
                request.maxBudget(),
                request.duration(),
                request.maxProposalCount()
        );

        // 7. 수정된 프로젝트를 DTO로 변환하여 반환
        return convertToDto(project);
    }

    /**
     * 프로젝트 삭제 (본인 CLIENT만 가능)
     * - @SoftDelete 어노테이션에 의해 실제 DB에서 삭제되지 않고 deleted = true 로 변경됩니다.
     */
    @Transactional
    public void deleteProject(Long id, String userEmail) {

        // 1. 삭제할 프로젝트 존재 여부 검증
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        // 2. 요청한 사용자 존재 여부 검증
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        // 3. 요청한 사용자의 Client 역할 검증
        Client client = clientRepository.findByUser(user)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        // 4. 프로젝트 소유자 본인 여부 검증
        if (!project.getClient().getId().equals(client.getId())) {
            throw new ProjectException(ErrorCode.USER_FORBIDDEN);
        }

        // 5. 프로젝트 삭제 (SoftDelete → deleted = true 로 변경)
        projectRepository.delete(project);
    }

    /**
     * 제안서 수 증가 (ProposalService에서 호출)
     * - 새로운 제안서가 제출될 때마다 currentProposalCount를 1 증가
     * - currentProposalCount가 maxProposalCount에 도달하면 status를 CLOSED로 변경
     * - 이미 CLOSED 상태인 프로젝트에는 제안서를 제출할 수 없음
     *
     * 추후 Redis Lock을 통해 동시성 제어가 적용될 예정
     */
    @Transactional
    public void incrementProposalCount(Long projectId) {

        // 1단계: 프로젝트 존재 여부 검증
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        // 2단계: 이미 마감된 프로젝트인지 검증
        // OPEN 상태가 아니라면 차단
        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new ProjectException(ErrorCode.PROJECT_ALREADY_CLOSED);
        }

        // 3단계: 제안서 수 증가
        // currentProposalCount가 maxProposalCount에 도달하면
        // 엔티티 내부에서 자동으로 status = CLOSED 로 변경됨
        project.increaseProposalCount();
    }

    /**
     * 프로젝트 엔티티 → ProjectDto 변환
     * - Entity가 Controller 밖으로 노출되지 않도록 DTO로 변환
     */
    private ProjectDto convertToDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getCategory().getName(),
                project.getMinBudget(),
                project.getMaxBudget(),
                project.getDuration(),
                project.getSkills(),
                project.getStatus().name(),
                project.getCurrentProposalCount(),
                project.getMaxProposalCount(),
                project.getClient().getUser().getName(),
                project.getClient().getRating(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
