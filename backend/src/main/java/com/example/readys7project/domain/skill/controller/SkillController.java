package com.example.readys7project.domain.skill.controller;

import com.example.readys7project.domain.skill.dto.request.CreateSkillRequestDto;
import com.example.readys7project.domain.skill.dto.SkillDto;
import com.example.readys7project.domain.skill.dto.request.UpdateSkillRequestDto;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.skill.service.SkillService;
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

@RestController
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping("/v1/skills")
    public ResponseEntity<ApiResponseDto<SkillDto>> createSkill(
            @Valid @RequestBody CreateSkillRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, skillService.createSkill(request, email)));
    }

    @GetMapping("/v1/skills")
    public ResponseEntity<ApiResponseDto<Page<SkillDto>>> getSkills(
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, skillService.getSkills(pageable)));
    }

    @GetMapping("/v1/skills/search")
    public ResponseEntity<ApiResponseDto<Page<SkillDto>>> searchSkills(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) SkillCategory category,
            Pageable pageable
    ) {
       return ResponseEntity
               .ok(ApiResponseDto.success(HttpStatus.OK, skillService.searchSkills(name, category, pageable)));
    }

    @PatchMapping("/v1/skills/{skillId}")
    public ResponseEntity<ApiResponseDto<SkillDto>> updateSkill(
            @PathVariable Long skillId,
            @RequestBody UpdateSkillRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity
                .ok(ApiResponseDto.success(HttpStatus.OK, skillService.updateSkill(skillId, request, email)));
    }

    @DeleteMapping("/v1/skills/{skillId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSkill(
            @PathVariable Long skillId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        skillService.deleteSkill(skillId, email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }
}
