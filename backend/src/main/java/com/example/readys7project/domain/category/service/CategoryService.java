package com.example.readys7project.domain.category.service;

import com.example.readys7project.domain.category.dto.CategoryDto;
import com.example.readys7project.domain.category.dto.request.CategoryRequestDto;
import com.example.readys7project.domain.category.dto.request.CategoryUpdateRequestDto;
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
    public CategoryDto createCategory(CategoryRequestDto request, String email) {

        // 1. 요청한 사용자 존재 여부 검증
        User user = findUser(email);

        // 2. 사용자 ADMIN 역할 여부 검증
        validateAdmin(user);

        // 3. ADMIN 역할의 유저 불러오기
        Admin admin = findAdmin(user);

        // 4. 불러온 ADMIN의 승인 상태 검증
        validateAdminStatus(admin);

        // 5. 카테고리 이름 중복 검증
        if (categoryRepository.existsByName(request.name())) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        // 6. 카테고리 생성
        Category category = Category.builder()
                .name(request.name())
                .icon(request.icon())
                .description(request.description())
                .displayOrder(request.displayOrder())
                .build();

        // 7. 저장 후 DTO로 변환하여 반환
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
    public List<CategoryDto> searchCategories(String name) {

        // 이름 기준 LIKE 검색 후 DTO 변환하여 반환
        return categoryRepository.findByNameContainingWithAdmin(name).stream()
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
    public CategoryDto updateCategory(Long categoryId, CategoryUpdateRequestDto request, String email) {

        // 1. 요청한 사용자 존재 여부 검증
        User user = findUser(email);

        // 2. 사용자 ADMIN 역할 여부 검증
        validateAdmin(user);

        // 3. ADMIN 역할의 유저 불러오기
        Admin admin = findAdmin(user);

        // 4. 불러온 ADMIN의 승인 상태 검증
        validateAdminStatus(admin);

        // 5. 수정할 카테고리 존재 여부 검증
        Category category = findCategory(categoryId);

        // 6. 변경하려는 이름이 다른 카테고리와 중복되는지 검증
        // 현재 카테고리 이름과 동일한 경우는 중복으로 처리하지 않음
        if (!category.getName().equals(request.name())
                 && categoryRepository.existsByName(request.name())) {
            throw new CategoryException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        // 7. 카테고리 정보 업데이트
        // Dirty Checking에 의해 트랜잭션 종료 시 자동으로 UPDATE 쿼리가 실행됨
        category.updateCategory(
                request.name(),
                request.icon(),
                request.description(),
                request.displayOrder()
        );

        // 8. 수정된 카테고리를 DTO로 변환하여 반환
        return convertToDto(category);
    }

    /**
     * 카테고리 삭제 (ADMIN 전용)
     * - ADMIN 역할을 가진 사용자만 카테고리 삭제 가능
     */
    @Transactional
    public void deleteCategory(Long categoryId, String email) {

        // 1. 요청한 사용자 존재 여부 검증
       User user = findUser(email);

        // 2. 사용자 ADMIN 역할 여부 검증
        validateAdmin(user);

        // 3. ADMIN 역할의 유저 불러오기
        Admin admin = findAdmin(user);

        // 4. ADMIN의 승인 상태 검증
        validateAdminStatus(admin);

        // 5. 삭제할 카테고리 존재 여부 검증
        Category category = findCategory(categoryId);

        // 6. 카테고리 삭제
        categoryRepository.delete(category);
    }

    /**
     * 카테고리 엔티티 -> CategoryDto 변환
     * - Entity가 Controller 밖으로 노출되지 않도록 DTO로 반환
     */
    private CategoryDto convertToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .adminId(category.getAdmin().getId())
                .name(category.getName())
                .icon(category.getIcon())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .build();
    }

    private User findUser(String email) {
        // 요청한 사용자 존재 여부 검증
        return userRepository.findByEmail(email).orElseThrow(
                () -> new CategoryException(ErrorCode.USER_NOT_FOUND)
        );
    }

    private Admin findAdmin(User user) {
        // ADMIN 역할의 유저 불러오기
        return adminRepository.findByUser(user).orElseThrow(
                () -> new CategoryException(ErrorCode.ADMIN_NOT_FOUND)
        );
    }

    private Category findCategory(Long categoryId) {
        // 카테고리 존재 여부 검증
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validateAdmin(User user) {
        // 요청한 사용자의 ADMIN 역할 검증
        if (!user.getUserRole().equals(UserRole.ADMIN)) {
            throw new CategoryException(ErrorCode.USER_FORBIDDEN);
        }
    }

    private void validateAdminStatus(Admin admin) {
        // ADMIN의 승인 상태 검증
        if (!admin.getStatus().equals(AdminStatus.APPROVED)) {
            throw new CategoryException(ErrorCode.ADMIN_NOT_APPROVED);
        }
    }
}
