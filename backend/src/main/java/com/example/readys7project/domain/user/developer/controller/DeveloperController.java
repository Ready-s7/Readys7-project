package com.example.readys7project.domain.user.developer.controller;

import com.example.readys7project.domain.project.dto.ProjectDto;
import com.example.readys7project.domain.user.developer.dto.DeveloperDto;
import com.example.readys7project.domain.user.developer.dto.request.DeveloperProfileRequestDto;
import com.example.readys7project.domain.user.developer.service.DeveloperService;
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
public class DeveloperController {

    private final DeveloperService developerService;

    // 전체 개발자 목록
    @GetMapping("/v1/developers")
    public ResponseEntity<ApiResponseDto<Page<DeveloperDto>>> getAllDevelopers(Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.getAllDevelopers(pageable)));
    }

    // 내 프로젝트 목록 조회
    @GetMapping("/v1/developers/me/my-projects")
    public ResponseEntity<ApiResponseDto<Page<ProjectDto>>> getMyProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        String email = userDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.getMyProjects(email, pageable)));
    }

    // 개발자 상세 조회
    @GetMapping("/v1/developers/{developerId}")
    public ResponseEntity<ApiResponseDto<DeveloperDto>> getDeveloperById(@PathVariable Long developerId) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.getDeveloperById(developerId)));
    }

    // 개발자 검색 (skill, minRating)
    @GetMapping("/v1/developers/search")
    public ResponseEntity<ApiResponseDto<Page<DeveloperDto>>> searchDevelopers(
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Double minRating,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.searchDevelopers(
                skills, minRating, pageable)));
    }

    // 개발자 프로필 수정 (DEVELOPER 전용)
    @PutMapping("/v1/developers/profile")
    public ResponseEntity<ApiResponseDto<DeveloperDto>> updateProfile(
            @Valid @RequestBody DeveloperProfileRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.updateProfile(request, email)));
    }
}
