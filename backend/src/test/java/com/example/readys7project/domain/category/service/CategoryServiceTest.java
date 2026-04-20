package com.example.readys7project.domain.category.service;

import com.example.readys7project.domain.category.dto.CategoryResponseDto;
import com.example.readys7project.domain.category.dto.request.CategoryCreateRequestDto;
import com.example.readys7project.domain.category.dto.request.CategoryUpdateRequestDto;
import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.CategoryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @InjectMocks
    private CategoryService categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AdminRepository adminRepository;

    @Nested
    @DisplayName("카테고리 생성 테스트")
    class CreateCategory {

        @Test
        @DisplayName("성공: 카테고리 생성")
        void createCategory_Success() {
            // given
            String email = "admin@email.com";
            CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                    .name("개발")
                    .icon("icon")
                    .description("설명")
                    .displayOrder(1)
                    .build();

            Admin admin = Admin.builder().build();
            ReflectionTestUtils.setField(admin, "id", 1L);

            given(adminRepository.findByUserEmail(email)).willReturn(Optional.of(admin));
            given(categoryRepository.existsByName(request.name())).willReturn(false);
            
            Category savedCategory = Category.builder()
                    .name(request.name())
                    .icon(request.icon())
                    .description(request.description())
                    .displayOrder(request.displayOrder())
                    .admin(admin)
                    .build();
            ReflectionTestUtils.setField(savedCategory, "id", 1L);
            
            given(categoryRepository.save(any(Category.class))).willReturn(savedCategory);

            // when
            CategoryResponseDto result = categoryService.createCategory(request, email);

            // then
            assertThat(result.name()).isEqualTo(request.name());
            assertThat(result.adminId()).isEqualTo(admin.getId());
            verify(categoryRepository, times(1)).save(any(Category.class));
        }

        @Test
        @DisplayName("실패: 관리자를 찾을 수 없음")
        void createCategory_AdminNotFound() {
            // given
            String email = "notfound@email.com";
            CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                    .name("개발")
                    .displayOrder(1)
                    .build();

            given(adminRepository.findByUserEmail(email)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(request, email))
                    .isInstanceOf(CategoryException.class)
                    .hasMessage(ErrorCode.ADMIN_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: 이미 존재하는 카테고리 이름")
        void createCategory_CategoryAlreadyExists() {
            // given
            String email = "admin@email.com";
            CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                    .name("개발")
                    .displayOrder(1)
                    .build();

            Admin admin = Admin.builder().build();
            given(adminRepository.findByUserEmail(email)).willReturn(Optional.of(admin));
            given(categoryRepository.existsByName(request.name())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryService.createCategory(request, email))
                    .isInstanceOf(CategoryException.class)
                    .hasMessage(ErrorCode.CATEGORY_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("카테고리 조회 테스트")
    class GetCategories {

        @Test
        @DisplayName("성공: 전체 카테고리 조회")
        void getAllCategories_Success() {
            // given
            Admin admin = Admin.builder().build();
            ReflectionTestUtils.setField(admin, "id", 1L);

            Category category1 = Category.builder().name("카테고리1").displayOrder(1).admin(admin).build();
            Category category2 = Category.builder().name("카테고리2").displayOrder(2).admin(admin).build();
            ReflectionTestUtils.setField(category1, "id", 1L);
            ReflectionTestUtils.setField(category2, "id", 2L);

            given(categoryRepository.findAllWithAdminOrderByDisplayOrderAsc()).willReturn(List.of(category1, category2));

            // when
            List<CategoryResponseDto> result = categoryService.getAllCategories();

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("카테고리1");
            assertThat(result.get(1).name()).isEqualTo("카테고리2");
        }

        @Test
        @DisplayName("성공: 카테고리 검색")
        void searchCategories_Success() {
            // given
            Admin admin = Admin.builder().build();
            ReflectionTestUtils.setField(admin, "id", 1L);

            Category category = Category.builder().name("검색어").displayOrder(1).admin(admin).build();
            ReflectionTestUtils.setField(category, "id", 1L);

            given(categoryRepository.searchCategories("검색어", null)).willReturn(List.of(category));

            // when
            List<CategoryResponseDto> result = categoryService.searchCategories("검색어", null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("검색어");
        }
    }

    @Nested
    @DisplayName("카테고리 수정 테스트")
    class UpdateCategory {

        @Test
        @DisplayName("성공: 카테고리 수정")
        void updateCategory_Success() {
            // given
            Long categoryId = 1L;
            CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder()
                    .name("수정된이름")
                    .displayOrder(5)
                    .build();

            Admin admin = Admin.builder().build();
            ReflectionTestUtils.setField(admin, "id", 1L);

            Category category = Category.builder().name("기존이름").displayOrder(1).admin(admin).build();
            ReflectionTestUtils.setField(category, "id", categoryId);

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
            given(categoryRepository.existsByName(request.name())).willReturn(false);

            // when
            CategoryResponseDto result = categoryService.updateCategory(categoryId, request);

            // then
            assertThat(result.name()).isEqualTo("수정된이름");
            assertThat(result.displayOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("실패: 수정하려는 이름이 이미 존재함")
        void updateCategory_AlreadyExists() {
            // given
            Long categoryId = 1L;
            CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder()
                    .name("이미있는이름")
                    .build();

            Admin admin = Admin.builder().build();
            Category category = Category.builder().name("기존이름").admin(admin).build();
            ReflectionTestUtils.setField(category, "id", categoryId);

            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));
            given(categoryRepository.existsByName(request.name())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, request))
                    .isInstanceOf(CategoryException.class)
                    .hasMessage(ErrorCode.CATEGORY_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("카테고리 삭제 테스트")
    class DeleteCategory {

        @Test
        @DisplayName("성공: 카테고리 삭제")
        void deleteCategory_Success() {
            // given
            Long categoryId = 1L;
            Category category = Category.builder().name("삭제할거").build();
            given(categoryRepository.findById(categoryId)).willReturn(Optional.of(category));

            // when
            categoryService.deleteCategory(categoryId);

            // then
            verify(categoryRepository, times(1)).delete(category);
        }

        @Test
        @DisplayName("실패: 삭제할 카테고리가 없음")
        void deleteCategory_NotFound() {
            // given
            Long categoryId = 999L;
            given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                    .isInstanceOf(CategoryException.class)
                    .hasMessage(ErrorCode.CATEGORY_NOT_FOUND.getMessage());
        }
    }
}
