package com.example.readys7project.domain.user.developer.controller;

import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.user.developer.dto.DeveloperResponseDto;
import com.example.readys7project.domain.user.developer.dto.request.DeveloperProfileRequestDto;
import com.example.readys7project.domain.user.developer.service.DeveloperService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    public ResponseEntity<ApiResponseDto<Page<DeveloperResponseDto>>> getAllDevelopers(Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.getAllDevelopers(adjustPageable(pageable))));
    }

    // 내 프로젝트 목록 조회
    @GetMapping("/v1/developers/me/my-projects")
    public ResponseEntity<ApiResponseDto<Page<ProjectResponseDto>>> getMyProjects(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        String email = userDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.getMyProjects(email, adjustPageable(pageable))));
    }

    // 개발자 상세 조회
    @GetMapping("/v1/developers/{developerId}")
    public ResponseEntity<ApiResponseDto<DeveloperResponseDto>> getDeveloperById(@PathVariable Long developerId) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.getDeveloperById(developerId)));
    }

    // 개발자 검색 (skill, minRating)
    @GetMapping("/v1/developers/search")
    public ResponseEntity<ApiResponseDto<Page<DeveloperResponseDto>>> searchDevelopers(
            @RequestParam(required = false) List<String> skills,
            @RequestParam(required = false) Double minRating,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.searchDevelopers(
                skills, minRating, adjustPageable(pageable))));
    }

    // 개발자 프로필 수정 (DEVELOPER 전용)
    @PutMapping("/v1/developers/profile")
    public ResponseEntity<ApiResponseDto<DeveloperResponseDto>> updateProfile(
            @Valid @RequestBody DeveloperProfileRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.updateProfile(request, email)));
    }

    /**
     * 페이지 번호 보정 (1-based -> 0-based)
     * 프론트에서 1을 보내면 0으로, 0을 보내면 0으로 처리하여 1페이지 버그 방지
     */
    private Pageable adjustPageable(Pageable pageable) {
        int page = pageable.getPageNumber() > 0 ? pageable.getPageNumber() - 1 : 0;
        return PageRequest.of(page, pageable.getPageSize(), pageable.getSort());
    }
}
