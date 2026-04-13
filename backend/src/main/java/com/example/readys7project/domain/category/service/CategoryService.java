package com.example.readys7project.domain.category.service;

import com.example.readys7project.domain.category.dto.CategoryDto;
import com.example.readys7project.domain.category.dto.request.CategoryRequestDto;
import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    /**
     * 카테고리 생성 (ADMIN 전용)
     * - ADMIN 역할을 가진 사용자만 카테고리 생성 가능
     * - 카테고리 이름 중복을 허용하지 않음
     */
    @Transactional
    public CategoryDto createCategory(CategoryRequestDto request, String userEmail) {

        // 1. 요청한 사용자 존재 여부 검증
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CategoryException(ErrorCode.USER_NOT_FOUND));

        // 2. ADMIN 역할 여부 검증(CLIENT, DEVELOPER는 카테고리 생성 불가)
        if (user.getUserRole() != UserRole.ADMIN) {
            throw new CategoryException(ErrorCode.USER_FORBIDDEN);
        }

        // 3. Admin 테이블에서 APPROVED 상태인지 확인
        Admin admin = adminRepository.findByUser(user)
                .orElseThrow(() -> new CategoryException(ErrorCode.ADMIN_NOT_FOUND));

        if (admin.getStatus() != AdminStatus.APPROVED) {
            throw new CategoryException(ErrorCode.ADMIN_NOT_APPROVED);
        }

        // 4. 카테고리 이름 중복 검증
        if (categoryRepository.existsByName(request.name())) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        // 5. 카테고리 이름 중복 검증
        Category category = Category.builder()
                .name(request.name())
                .icon(request.icon())
                .description(request.description())
                .displayOrder(request.displayOrder())
                .build();

        // 6. 저장 후 DTO로 변환하여 반환
        return convertToDto(categoryRepository.save(category));
    }

    /**
     * 카테고리 전체 조회
     * - 인증 없이 누구나 조회 가능
     * - displayOrder 기준으로 정렬된 목록을 반환
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {

        // displayOrder 기준 오름차순 정렬하여 전체 조회 후 DTO 변환하여 반환
        return categoryRepository.findAll().stream()
                .sorted((a,b) -> a.getDisplayOrder().compareTo(b.getDisplayOrder()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 검색
     * - 카테고리 이름을 기준으로 LIKE 검색
     * - 인증 없이 누구나 검색 가능
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> searchCategories(String name) {

        // 이름 기준 LIKE 검색 후 DTO 변환하여 반환
        return categoryRepository.findByNameContaining(name).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리 수정 (ADMIN 전용)
     * - ADMIN 역할을 가진 사용자만 카테고리 수정 가능
     * - 수정하려는 이름이 다른 카테고리와 중복되면 예외 발생
     * - @Transactional 덕분에 별도 save() 호출 없이 변경사항이 자동 반영 (Dirty Checking)
     */
    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryRequestDto request, String userEmail) {

        // 1. 요청한 사용자 존재 여부 검증
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CategoryException(ErrorCode.USER_NOT_FOUND));

        // 2. ADMIN 역할 여부 검증
        if (user.getUserRole() != UserRole.ADMIN) {
            throw new CategoryException(ErrorCode.USER_FORBIDDEN);
        }

        // 2단계: Admin 테이블에서 APPROVED 상태인지 확인
        Admin admin = adminRepository.findByUser(user)
                .orElseThrow(() -> new CategoryException(ErrorCode.ADMIN_NOT_FOUND));

        if (admin.getStatus() != AdminStatus.APPROVED) {
            throw new CategoryException(ErrorCode.ADMIN_NOT_APPROVED);
        }

        // 3. 수정할 카테고리 존재 여부 검증
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

        // 4. 변경하려는 이름이 다른 카테고리와 중복되는지 검증
        // 현재 카테고리 이름과 동일한 경우는 중복으로 처리하지 않음
        if (!category.getName().equals(request.name())
                 && categoryRepository.existsByName(request.name())) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        // 5. 카테고리 정보 업데이트
        // Dirty Checking에 의해 트랜잭션 종료 시 자동으로 UPDATE 쿼리가 실행됨
        category.updateCategory(
                request.name(),
                request.icon(),
                request.description(),
                request.displayOrder()
        );

        // 6. 수정된 카테고리를 DTO로 변환하여 반환
        return convertToDto(category);
    }

    /**
     * 카테고리 삭제 (ADMIN 전용)
     * - ADMIN 역할을 가진 사용자만 카테고리 삭제 가능
     */
    @Transactional
    public void deleteCategory(Long categoryId, String userEmail) {

        // 1. 요청한 사용자 존재 여부 검증
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CategoryException(ErrorCode.USER_NOT_FOUND));

        // 2. ADMIN 역할 여부 검증
        if (user.getUserRole() != UserRole.ADMIN) {
            throw new CategoryException(ErrorCode.USER_FORBIDDEN);
        }

        Admin admin = adminRepository.findByUser(user)
                .orElseThrow(() -> new CategoryException(ErrorCode.ADMIN_NOT_FOUND));

        if (admin.getStatus() != AdminStatus.APPROVED) {
            throw new CategoryException(ErrorCode.ADMIN_NOT_APPROVED);
        }

        // 3. 삭제할 카테고리 존재 여부 검증
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

        // 4. 카테고리 삭제
        categoryRepository.delete(category);
    }

    /**
     * 카테고리 엔티티 -> CategoryDto 변환
     * - Entity가 Controller 밖으로 노출되지 않도록 DTO로 반환
     */
    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .build();
    }
}
