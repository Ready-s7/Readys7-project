package com.example.readys7project.domain.project.controller;

import com.example.readys7project.domain.project.dto.ProjectDto;
import com.example.readys7project.domain.project.dto.request.ProjectRequestDto;
import com.example.readys7project.domain.project.service.ProjectService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
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
    public ResponseEntity<ApiResponseDto<ProjectDto>> createProject(
            @Valid @RequestBody ProjectRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, projectService.createProject(request, email)));
    }

    // 프로젝트 전체 목록 조회
    @GetMapping("/v1/projects")
    public ResponseEntity<ApiResponseDto<List<ProjectDto>>> getAllProjects() {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.getAllProjects()));
    }

    // 프로젝트 단건 조회
    @GetMapping("/v1/projects/{projectId}")
    public ResponseEntity<ApiResponseDto<ProjectDto>> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.getProjectById(projectId)));
    }

    // 프로젝트 검색
    @GetMapping("/v1/projects/search")
    public ResponseEntity<ApiResponseDto<Page<ProjectDto>>> searchProjects(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) List<String> skill,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.searchProjects(categoryId, status, skill, pageable)));
    }

    // 프로젝트 수정 (본인 Client만 가능)
    @PutMapping("/v1/projects/{projectId}")
    public ResponseEntity<ApiResponseDto<ProjectDto>> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();         // 검증 추가
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, projectService.updateProject(projectId, request, email)));
    }

    // 프로젝트 삭제 (본인 Client만 가능)
    @DeleteMapping("/v1/projects/{projectId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();          // 검증 추가
        projectService.deleteProject(projectId, email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }
}
