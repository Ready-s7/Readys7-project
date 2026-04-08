package com.example.readys7project.domain.developer.controller;

import com.example.readys7project.domain.developer.dto.DeveloperDto;
import com.example.readys7project.domain.developer.dto.request.DeveloperProfileRequest;
import com.example.readys7project.domain.developer.service.DeveloperService;
import com.example.readys7project.global.dto.ApiResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/developers")
@RequiredArgsConstructor
public class DeveloperController {

    private final DeveloperService developerService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<DeveloperDto>>> getAllDevelopers() {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.getAllDevelopers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDto<DeveloperDto>> getDeveloperById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.getDeveloperById(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<List<DeveloperDto>>> searchDevelopers(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minRating) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.searchDevelopers(skill, location, minRating)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponseDto<DeveloperDto>> updateProfile(
            @Valid @RequestBody DeveloperProfileRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, developerService.updateProfile(request, email)));
    }
}
