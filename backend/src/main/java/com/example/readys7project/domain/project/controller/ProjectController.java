package com.example.readys7project.domain.project.controller;

import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.project.dto.request.ProjectCreateRequestDto;
import com.example.readys7project.domain.project.dto.request.ProjectStatusUpdateRequestDto;
import com.example.readys7project.domain.project.dto.request.ProjectUpdateRequestDto;
import com.example.readys7project.domain.project.service.ProjectService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import com.example.readys7project.global.aop.CheckOwnerOrAdmin;
import com.example.readys7project.global.aop.EntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // 프로젝트 등록
    @PostMapping("/v1/projects")
    public ResponseEntity<ApiResponseDto<ProjectResponseDto>> createProject(
            @Valid @RequestBody ProjectCreateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, projectService.createProject(request, email)));
    }

    // 프로젝트 전체 목록 조회
    @GetMapping("/v1/projects")
    public ResponseEntity<ApiResponseDto<List<ProjectResponseDto>>> getAllProjects() {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.getAllProjects()));
    }

    // 프로젝트 단건 조회
    @GetMapping("/v1/projects/{projectId}")
    public ResponseEntity<ApiResponseDto<ProjectResponseDto>> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.getProjectById(projectId)));
    }

    // 프로젝트 검색
    @GetMapping("/v1/projects/search")
    public ResponseEntity<ApiResponseDto<Page<ProjectResponseDto>>> searchProjects(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<String> skill,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.searchProjects(keyword, categoryId, status, skill, pageable)));
    }

    // 프로젝트 수정 (본인 Client만 가능)
    @PutMapping("/v1/projects/{projectId}")
    @CheckOwnerOrAdmin(type = EntityType.PROJECT, idParam = "projectId")
    public ResponseEntity<ApiResponseDto<ProjectResponseDto>> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.updateProject(projectId, request)));
    }

    // 프로젝트 상태 변경 (CLIENT 본인 / ADMIN)
    @PatchMapping("/v1/projects/{projectId}/status")
    @CheckOwnerOrAdmin(type = EntityType.PROJECT, idParam = "projectId")
    public ResponseEntity<ApiResponseDto<ProjectResponseDto>> changeProjectStatus(
            @PathVariable Long projectId,
            @RequestBody ProjectStatusUpdateRequestDto request
    ) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.changeProjectStatus(projectId, request.status())));
    }

    // 프로젝트 삭제 (본인 Client만 가능)
    @DeleteMapping("/v1/projects/{projectId}")
    @CheckOwnerOrAdmin(type = EntityType.PROJECT, idParam = "projectId")
    public ResponseEntity<ApiResponseDto<Void>> deleteProject(
            @PathVariable Long projectId
    ) {
        projectService.deleteProject(projectId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }
}
