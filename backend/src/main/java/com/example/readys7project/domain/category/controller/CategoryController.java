package com.example.readys7project.domain.category.controller;

import com.example.readys7project.domain.category.dto.CategoryDto;
import com.example.readys7project.domain.category.dto.request.CategoryRequestDto;
import com.example.readys7project.domain.category.dto.request.CategoryUpdateRequestDto;
import com.example.readys7project.domain.category.service.CategoryService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // 카테고리 생성 (ADMIN 만 가능)
    @PostMapping("/v1/categories")
    public ResponseEntity<ApiResponseDto<CategoryDto>> createCategory(
            @Valid@RequestBody CategoryRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED,categoryService.createCategory(request, email)));
    }

    // 카테고리 전체 조회
    @GetMapping("/v1/categories")
    public ResponseEntity<ApiResponseDto<List<CategoryDto>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, categoryService.getAllCategories()));
    }

    // 카테고리 검색
    @GetMapping("/v1/categories/search")
    public ResponseEntity<ApiResponseDto<List<CategoryDto>>> searchCategories(
            @RequestParam String name) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, categoryService.searchCategories(name)));
    }

    // 카테고리 수정 (ADMIN 만 가능)
    @PatchMapping("/v1/categories/{categoryId}")
    public ResponseEntity<ApiResponseDto<CategoryDto>> updateCategory(
            @PathVariable Long categoryId,
            @Valid@RequestBody CategoryUpdateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, categoryService.updateCategory(categoryId, request, email)));
    }

    // 카테고리 삭제 (ADMIN 만 가능)
    @DeleteMapping("/v1/categories/{categoryId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        categoryService.deleteCategory(categoryId, email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponseDto.successWithNoContent());
    }
}
