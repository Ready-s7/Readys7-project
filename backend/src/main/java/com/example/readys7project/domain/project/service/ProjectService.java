package com.example.readys7project.domain.project.service;

import com.example.readys7project.domain.project.dto.ProjectDto;
import com.example.readys7project.domain.project.dto.request.ProjectRequest;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProjectException;
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

    @Transactional
    public ProjectDto createProject(ProjectRequest request, String userEmail) {
        User client = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        if (client.getRole() != UserRole.CLIENT) {
            throw new ProjectException(ErrorCode.USER_FORBIDDEN);
        }

        Project project = Project.builder()
                .client(client)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .budget(request.getBudget())
                .duration(request.getDuration())
                .skills(request.getSkills())
                .projectStatus(ProjectStatus.OPEN)
                .proposalCount(0)
                .build();

        project = projectRepository.save(project);
        return convertToDto(project);
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

    @Transactional(readOnly = true)
    public List<ProjectDto> searchProjects(String category, String status, String skill) {
        ProjectStatus projectStatus = status != null ?
            ProjectStatus.valueOf(status.toUpperCase()) : null;

        return projectRepository.searchProjects(category, projectStatus, skill).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDto updateProject(Long id, ProjectRequest request, String userEmail) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getClient().getId().equals(user.getId())) {
            throw new ProjectException(ErrorCode.USER_FORBIDDEN);
        }

        project.update(request);

        project = projectRepository.save(project);
        return convertToDto(project);
    }

    @Transactional
    public void deleteProject(Long id, String userEmail) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProjectException(ErrorCode.USER_NOT_FOUND));

        if (!project.getClient().getId().equals(user.getId())) {
            throw new ProjectException(ErrorCode.USER_FORBIDDEN);
        }

        projectRepository.delete(project);
    }

    @Transactional
    public void incrementProposalCount(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND));
        project.setProposalCount(project.getProposalCount() + 1);
        projectRepository.save(project);
    }

    private ProjectDto convertToDto(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .category(project.getCategory())
                .budget(project.getBudget())
                .duration(project.getDuration())
                .skills(project.getSkills())
                .status(project.getStatus().name().toLowerCase())
                .proposalCount(project.getProposalCount())
                .clientName(project.getClient().getName())
                .clientRating(4.5) // TODO: 실제 클라이언트 평점 계산
                .build();
    }
}
