package com.example.readys7project.domain.project.service;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.project.dto.request.ProjectCreateRequestDto;
import com.example.readys7project.domain.project.dto.request.ProjectUpdateRequestDto;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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

    /**
     * 프로젝트 생성 (CLIENT 역할을 가진 사용자만 가능)
     */
    @Transactional
    @CacheEvict(value = "globalSearch", allEntries = true)
    public ProjectResponseDto createProject(ProjectCreateRequestDto request, String email) {

        if (request.minBudget() > request.maxBudget()) {
            throw new ProjectException(ErrorCode.PROJECT_BUDGET_BAD_REQUEST);
        }

        User user = findUser(email);

        if (user.getUserRole() != UserRole.CLIENT) {
            throw new ProjectException(ErrorCode.USER_FORBIDDEN);
        }

        Client client = validateClient(user);
        Category category = findCategory(request.categoryId());

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

        return convertToDto(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponseDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponseDto getProjectById(Long id) {
        Project project = findProject(id);
        return convertToDto(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponseDto> searchProjects(String keyword, Long categoryId, String status, List<String> skill, Pageable pageable) {
        Category category = categoryId != null ? findCategory(categoryId) : null;
        ProjectStatus projectStatus = status != null ? ProjectStatus.valueOf(status.toUpperCase()) : null;

        return projectRepository.searchProjects(keyword, category, projectStatus, skill, pageable)
                .map(this::convertToDto);
    }

    /**
     * 프로젝트 수정 (본인 Client만 가능 - AOP에서 검증)
     */
    @Transactional
    @CacheEvict(value = "globalSearch", allEntries = true)
    public ProjectResponseDto updateProject(Long id, ProjectUpdateRequestDto request) {
        Project project = findProject(id);

        Category category = request.categoryId() != null
                ? findCategory(request.categoryId())
                : project.getCategory();

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

        return convertToDto(project);
    }

    /**
     * 프로젝트 삭제 (본인 Client만 가능 - AOP에서 검증)
     */
    @Transactional
    @CacheEvict(value = "globalSearch", allEntries = true)
    public void deleteProject(Long id) {
        Project project = findProject(id);
        projectRepository.delete(project);
    }

    @Transactional
    public void incrementProposalCount(Long projectId) {
        Project project = findProject(projectId);

        if (project.getStatus() != ProjectStatus.OPEN) {
            throw new ProjectException(ErrorCode.PROJECT_ALREADY_CLOSED);
        }

        project.increaseProposalCount();
    }

    /**
     * 프로젝트 상태 변경 (AOP에서 소유권/관리자 검증)
     */
    @Transactional
    @CacheEvict(value = "globalSearch", allEntries = true)
    public ProjectResponseDto changeProjectStatus(Long projectId, String statusStr) {
        Project project = findProject(projectId);
        if (!(statusStr.equalsIgnoreCase("COMPLETED")
                || statusStr.equalsIgnoreCase("CANCELLED"))) {
            throw new ProjectException(ErrorCode.INVALID_INPUT);
        }
        ProjectStatus newStatus = ProjectStatus.valueOf(statusStr.toUpperCase());

        validateStatusTransition(project.getStatus(), newStatus);
        project.changeStatus(newStatus);

        return convertToDto(project);
    }

    private void validateStatusTransition(ProjectStatus current, ProjectStatus next) {
        boolean allowed = switch (current) {
            case OPEN, CLOSED -> next == ProjectStatus.IN_PROGRESS || next == ProjectStatus.CANCELLED;
            case IN_PROGRESS -> next == ProjectStatus.COMPLETED   || next == ProjectStatus.CANCELLED;
            default -> false;
        };

        if (!allowed) {
            throw new ProjectException(ErrorCode.PROJECT_STATUS_UPDATE_FAILED);
        }
    }

    private ProjectResponseDto convertToDto(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getClient().getId(),
                project.getClient().getUser().getId(),
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

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));
    }

    private Category findCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProjectException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.isDeleted()) {
            throw new ProjectException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return category;
    }

    private Client validateClient(User user) {
        return clientRepository.findByUser(user)
                .orElseThrow(() -> new ProjectException(ErrorCode.CLIENT_NOT_FOUND));
    }
}
