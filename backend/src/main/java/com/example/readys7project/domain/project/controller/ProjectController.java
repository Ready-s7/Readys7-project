package com.example.readys7project.domain.project.controller;

import com.example.readys7project.domain.project.dto.ProjectDto;
import com.example.readys7project.domain.project.dto.request.ProjectRequest;
import com.example.readys7project.domain.project.service.ProjectService;
import com.example.readys7project.global.dto.ApiResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<ProjectDto>> createProject(
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, projectService.createProject(request, email)));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ProjectDto>>> getAllProjects() {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, projectService.getAllProjects()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProjectDto>> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, projectService.getProjectById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<List<ProjectDto>>> searchProjects(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String skill) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, projectService.searchProjects(category, status, skill)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<ProjectDto>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, projectService.updateProject(id, request, email)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteProject(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        projectService.deleteProject(id, email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponseDto.successWithNoContent());
    }
}
