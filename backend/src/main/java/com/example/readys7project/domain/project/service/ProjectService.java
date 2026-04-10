package com.example.readys7project.domain.project.service;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.dto.ProjectDto;
import com.example.readys7project.domain.project.dto.request.ProjectRequestDto;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.CategoryException;
import com.example.readys7project.global.exception.domain.ProjectException;
import com.example.readys7project.global.exception.domain.UserException;
import lombok.RequiredArgsConstructor;
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

    // 프로젝트 생성
    @Transactional
    public ProjectDto createProject(ProjectRequestDto request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (user.getUserRole() != UserRole.CLIENT) {
            throw new UserException(ErrorCode.USER_FORBIDDEN);
        }
        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

        Project project = Project.builder()
                .client(client)
                .category(category)
                .title(request.title())
                .description(request.description())
                .skills(convertSkillsToJson(request.skills()))
                .budget(request.budget())
                .duration(request.duration())
                .maxProposals(request.maxProposals())
                .recruitDeadline(request.recruitDeadline())
                .build();

        return convertToDto(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));
        return convertToDto(project);
    }

    // 프로젝트 검색
    @Transactional(readOnly = true)
    public List<ProjectDto> searchProjects(String category, String status, String skill) {
        ProjectStatus projectStatus = status != null ?
            ProjectStatus.valueOf(status.toUpperCase()) : null;

        return projectRepository.searchProjects(category, projectStatus, skill).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 내 프로젝트 조회
    @Transactional(readOnly = true)
    public List<ProjectDto> getMyProjects(String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));


        return projectRepository.findByClientId(user.getId()).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Transactional
    public ProjectDto updateProject(Long id, ProjectRequestDto request, String userEmail) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        Client client = clientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));


        if (!project.getClient().getId().equals(user.getId())) {
            throw new UserException(ErrorCode.USER_FORBIDDEN);
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

        project.update(
                request.title(),
                request.description(),
                category,
                convertSkillsToJson(request.skills()),
                request.budget(),
                request.duration(),
                request.maxProposals(),
                request.recruitDeadline()
        );

        return convertToDto(project);
    }

    // 프로젝트 삭제
    @Transactional
    public void deleteProject(Long id, String userEmail) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        if (!project.getClient().getId().equals(user.getId())) {
            throw new UserException(ErrorCode.USER_FORBIDDEN);
        }

        projectRepository.delete(project);
    }

    // DTO 변환
    private ProjectDto convertToDto(Project project) {
        return new ProjectDto(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getCategory().getName(),
                project.getBudget(),
                project.getDuration(),
                convertJsonToSkills(project.getSkills()),
                project.getStatus().name(),
                project.getCurrentProposals(),
                project.getMaxProposals(),
                project.getClient().getUser().getName(),
                project.getClient().getRating(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    // JSON 변환
    private String convertSkillsToJson(List<String> skills) {
        if (skills == null || skills.isEmpty()) return "[]";
        return skills.toString(); // 간단 버전 (추후 ObjectMapper 추천)
    }

    private List<String> convertJsonToSkills(String json) {
        if (json == null || json.equals("[]")) return List.of();
        return List.of(json.replace("[", "").replace("]", "").split(", "));
    }
}
