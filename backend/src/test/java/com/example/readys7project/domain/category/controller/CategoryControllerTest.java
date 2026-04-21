package com.example.readys7project.domain.category.controller;

import com.example.readys7project.domain.category.dto.CategoryResponseDto;
import com.example.readys7project.domain.category.dto.request.CategoryCreateRequestDto;
import com.example.readys7project.domain.category.dto.request.CategoryUpdateRequestDto;
import com.example.readys7project.domain.category.service.CategoryService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.CategoryException;
import com.example.readys7project.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private CategoryController categoryController;

    @Mock
    private CategoryService categoryService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        User user = User.builder().email("admin@email.com").build();
                        ReflectionTestUtils.setField(user, "id", 1L);
                        return new CustomUserDetails(user);
                    }
                })
                .build();
    }

    @Nested
    @DisplayName("카테고리 생성 API 테스트")
    class CreateCategory {
        @Test
        @DisplayName("성공: 카테고리 생성")
        void createCategory_Success() throws Exception {
            // given
            CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                    .name("개발")
                    .displayOrder(1)
                    .build();
            CategoryResponseDto response = CategoryResponseDto.builder()
                    .id(1L)
                    .name("개발")
                    .displayOrder(1)
                    .build();
            given(categoryService.createCategory(any(), anyString())).willReturn(response);

            // when & then
            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("개발"));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 카테고리")
        void createCategory_Failure_AlreadyExists() throws Exception {
            // given
            CategoryCreateRequestDto request = CategoryCreateRequestDto.builder()
                    .name("이미있는")
                    .displayOrder(1)
                    .build();
            given(categoryService.createCategory(any(), anyString()))
                    .willThrow(new CategoryException(ErrorCode.CATEGORY_ALREADY_EXISTS));

            // when & then
            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"));
        }
    }

    @Nested
    @DisplayName("카테고리 조회 API 테스트")
    class GetCategories {
        @Test
        @DisplayName("성공: 전체 조회")
        void getAllCategories_Success() throws Exception {
            // given
            CategoryResponseDto category = CategoryResponseDto.builder().id(1L).name("카테고리").build();
            given(categoryService.getAllCategories()).willReturn(List.of(category));

            // when & then
            mockMvc.perform(get("/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("카테고리"));
        }

        @Test
        @DisplayName("성공: 검색")
        void searchCategories_Success() throws Exception {
            // given
            CategoryResponseDto category = CategoryResponseDto.builder().id(1L).name("검색결과").build();
            given(categoryService.searchCategories(anyString(), any())).willReturn(List.of(category));

            // when & then
            mockMvc.perform(get("/v1/categories/search")
                            .param("name", "검색"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].name").value("검색결과"));
        }
    }

    @Nested
    @DisplayName("카테고리 수정 API 테스트")
    class UpdateCategory {
        @Test
        @DisplayName("성공: 카테고리 수정")
        void updateCategory_Success() throws Exception {
            // given
            Long categoryId = 1L;
            CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder().name("수정됨").build();
            CategoryResponseDto response = CategoryResponseDto.builder().id(1L).name("수정됨").build();
            given(categoryService.updateCategory(eq(categoryId), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/v1/categories/{categoryId}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("수정됨"));
        }

        @Test
        @DisplayName("실패: 카테고리 없음")
        void updateCategory_Failure_NotFound() throws Exception {
            // given
            Long categoryId = 999L;
            CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder().name("수정").build();
            given(categoryService.updateCategory(eq(categoryId), any()))
                    .willThrow(new CategoryException(ErrorCode.CATEGORY_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/v1/categories/{categoryId}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        }

        @Test
        @DisplayName("실패: CLIENT가 카테고리 수정 시도 → 403 Forbidden")
        void updateCategory_Failure_Forbidden() throws Exception {
            // given
            // @AdminOnly AOP가 던질 예외를 Service Mock에서 재현
            Long categoryId = 1L;
            CategoryUpdateRequestDto request = CategoryUpdateRequestDto.builder().name("수정").build();
            given(categoryService.updateCategory(eq(categoryId), any()))
                    .willThrow(new CategoryException(ErrorCode.USER_FORBIDDEN));

            // when & then
            mockMvc.perform(patch("/v1/categories/{categoryId}", categoryId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        }
    }

    @Nested
    @DisplayName("카테고리 삭제 API 테스트")
    class DeleteCategory {
        @Test
        @DisplayName("성공: 카테고리 삭제")
        void deleteCategory_Success() throws Exception {
            // given
            Long categoryId = 1L;

            // when & then
            mockMvc.perform(delete("/v1/categories/{categoryId}", categoryId))
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("실패: 삭제 대상 없음")
        void deleteCategory_Failure_NotFound() throws Exception {
            // given
            Long categoryId = 999L;
            doThrow(new CategoryException(ErrorCode.CATEGORY_NOT_FOUND))
                    .when(categoryService).deleteCategory(categoryId);

            // when & then
            mockMvc.perform(delete("/v1/categories/{categoryId}", categoryId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        }

        @Test
        @DisplayName("실패: CLIENT가 카테고리 삭제 시도 → 403 Forbidden")
        void deleteCategory_Failure_Forbidden() throws Exception {
            // given
            Long categoryId = 1L;
            doThrow(new CategoryException(ErrorCode.USER_FORBIDDEN))
                    .when(categoryService).deleteCategory(categoryId);

            // when & then
            mockMvc.perform(delete("/v1/categories/{categoryId}", categoryId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        }

        @Test
        @DisplayName("실패: 프로젝트에서 사용 중인 카테고리 삭제 시도 → 409 Conflict")
        void deleteCategory_Failure_CategoryInUse() throws Exception {
            // given
            Long categoryId = 1L;
            doThrow(new CategoryException(ErrorCode.CATEGORY_IN_USE))
                    .when(categoryService).deleteCategory(categoryId);

            // when & then
            mockMvc.perform(delete("/v1/categories/{categoryId}", categoryId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").doesNotExist())
                    .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"));
        }
    }
}
