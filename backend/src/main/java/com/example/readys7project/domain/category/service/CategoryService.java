package com.example.readys7project.domain.category.service;

import com.example.readys7project.domain.category.dto.CategoryResponseDto;
import com.example.readys7project.domain.category.dto.request.CategoryCreateRequestDto;
import com.example.readys7project.domain.category.dto.request.CategoryUpdateRequestDto;
import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.CategoryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProjectRepository projectRepository;
    private final AdminRepository adminRepository;

    /**
     * 카테고리 생성 (ADMIN 전용)
     * - ADMIN 역할을 가진 사용자만 카테고리 생성 가능 (Aspect에서 검증)
     * - 카테고리 이름 중복을 허용하지 않음
     */
    @Transactional
    public CategoryResponseDto createCategory(CategoryCreateRequestDto request, String email) {

        // ADMIN 역할의 유저 불러오기 (Aspect에서 검증 완료됨)
        Admin admin = adminRepository.findByUserEmail(email).orElseThrow(
                () -> new CategoryException(ErrorCode.ADMIN_NOT_FOUND)
        );

        // 카테고리 이름 중복 검증
        if (categoryRepository.existsByName(request.name())) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        // 카테고리 생성
        Category category = Category.builder()
                .name(request.name())
                .icon(request.icon())
                .description(request.description())
                .displayOrder(request.displayOrder())
                .admin(admin)
                .build();

        // 저장 후 DTO로 변환하여 반환
        return convertToDto(categoryRepository.save(category));
    }

    /**
     * 카테고리 전체 조회
     * - 인증 없이 누구나 조회 가능
     * - displayOrder 기준으로 정렬된 목록을 반환
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {

        // displayOrder 기준 오름차순 정렬하여 전체 조회 후 DTO 변환하여 반환
        return categoryRepository.findAllWithAdminOrderByDisplayOrderAsc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 검색
     * - 카테고리 이름을 기준으로 LIKE 검색
     * - 인증 없이 누구나 검색 가능
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> searchCategories(String name, String description) {
        return categoryRepository.searchCategories(name, description).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 수정 (ADMIN 전용)
     * - ADMIN 역할을 가진 사용자만 카테고리 수정 가능 (Aspect에서 검증)
     * - 수정하려는 이름이 다른 카테고리와 중복되면 예외 발생
     */
    @Transactional
    public CategoryResponseDto updateCategory(Long categoryId, CategoryUpdateRequestDto request) {

        // 수정할 카테고리 존재 여부 검증
        Category category = findCategory(categoryId);

        // 변경하려는 이름이 다른 카테고리와 중복되는지 검증
        if (!category.getName().equals(request.name())
                 && categoryRepository.existsByName(request.name())) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        // 카테고리 정보 업데이트
        category.updateCategory(
                request.name(),
                request.icon(),
                request.description(),
                request.displayOrder()
        );

        return convertToDto(category);
    }

    /**
     * 카테고리 삭제 (ADMIN 전용)
     * - ADMIN 역할을 가진 사용자만 카테고리 삭제 가능 (Aspect에서 검증)
     */
    @Transactional
    public void deleteCategory(Long categoryId) {

        // 삭제할 카테고리 존재 여부 검증
        Category category = findCategory(categoryId);

        if (projectRepository.existsByCategory(category)) {
            throw new CategoryException(ErrorCode.CATEGORY_IN_USE);
        }


        // 카테고리 삭제
        categoryRepository.delete(category);
    }

    private CategoryResponseDto convertToDto(Category category) {
        return CategoryResponseDto.builder()
                .id(category.getId())
                .adminId(category.getAdmin().getId())
                .name(category.getName())
                .icon(category.getIcon())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .build();
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
