package com.example.readys7project.domain.project.controller;

import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.project.dto.request.ProjectCreateRequestDto;
import com.example.readys7project.domain.project.dto.request.ProjectUpdateRequestDto;
import com.example.readys7project.domain.project.service.ProjectService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.ProjectException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ProjectController projectController;

    @Mock
    private ProjectService projectService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails customUserDetails;
    private ProjectResponseDto projectDto;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("client@example.com")
                .name("Test Client")
                .userRole(UserRole.CLIENT)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        customUserDetails = new CustomUserDetails(user);

        projectDto = ProjectResponseDto.builder()
                .id(1L)
                .clientId(1L)
                .clientUserId(1L)
                .title("Test Project")
                .description("Test Description")
                .category("Web")
                .minBudget(100L)
                .maxBudget(500L)
                .duration(30)
                .skills(List.of("Java"))
                .status("OPEN")
                .currentProposalCount(0)
                .maxProposalCount(10)
                .clientName("Test Client")
                .clientRating(4.5)
                .createdAt(LocalDateTime.now())
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().isAssignableFrom(CustomUserDetails.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return customUserDetails;
                            }
                        }
                )
                .build();
    }

    @Test
    @DisplayName("성공: 프로젝트 생성 API")
    void createProject_Success() throws Exception {
        // given
        ProjectCreateRequestDto request = new ProjectCreateRequestDto(
                "Test Project", "Test Description", 1L, 100L, 500L, 30, List.of("Java"), 10
        );
        given(projectService.createProject(any(ProjectCreateRequestDto.class), anyString())).willReturn(projectDto);

        // when & then
        mockMvc.perform(post("/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(projectDto.title()));
    }

    @Test
    @DisplayName("성공: 프로젝트 전체 조회 API")
    void getAllProjects_Success() throws Exception {
        // given
        given(projectService.getAllProjects()).willReturn(List.of(projectDto));

        // when & then
        mockMvc.perform(get("/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("성공: 프로젝트 단건 조회 API")
    void getProjectById_Success() throws Exception {
        // given
        given(projectService.getProjectById(1L)).willReturn(projectDto);

        // when & then
        mockMvc.perform(get("/v1/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("성공: 프로젝트 수정 API")
    void updateProject_Success() throws Exception {
        // given
        ProjectUpdateRequestDto request = new ProjectUpdateRequestDto(
                "Updated Title", "Updated Desc", 1L, 200L, 600L, 60, List.of("Kotlin"), 20
        );
        given(projectService.updateProject(eq(1L), any(ProjectUpdateRequestDto.class))).willReturn(projectDto);

        // when & then
        mockMvc.perform(put("/v1/projects/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Nested
    @DisplayName("프로젝트 검색 - 스킬 및 복합 조건 (200)")
    class SearchProjects {

        @Test
        @DisplayName("성공: 스킬 단독 검색 → 200 OK")
        void searchProjects_BySkillOnly_Success() throws Exception {
            // given
            // skill 파라미터만 전달, 나머지는 null
            Pageable pageable = PageRequest.of(0, 20);
            given(projectService.searchProjects(
                    isNull(), isNull(), isNull(), eq(List.of("Java")), any(Pageable.class))
            ).willReturn(new PageImpl<>(List.of(projectDto), pageable, 1));

            // when & then
            mockMvc.perform(get("/v1/projects/search")
                            .param("skill", "Java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].title").value("Test Project"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("성공: keyword + skill 복합 조건 검색 → 200 OK")
        void searchProjects_KeywordAndSkill_Success() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(projectService.searchProjects(
                    eq("Test"), isNull(), isNull(), eq(List.of("Java")), any(Pageable.class))
            ).willReturn(new PageImpl<>(List.of(projectDto), pageable, 1));

            // when & then
            mockMvc.perform(get("/v1/projects/search")
                            .param("keyword", "Test")
                            .param("skill", "Java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.content[0].skills[0]").value("Java"));
        }

        @Test
        @DisplayName("성공: keyword + categoryId + status + skill 전체 복합 조건 검색 → 200 OK")
        void searchProjects_AllConditions_Success() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(projectService.searchProjects(
                    eq("Test"), eq(1L), eq("OPEN"), eq(List.of("Java")), any(Pageable.class))
            ).willReturn(new PageImpl<>(List.of(projectDto), pageable, 1));

            // when & then
            mockMvc.perform(get("/v1/projects/search")
                            .param("keyword", "Test")
                            .param("categoryId", "1")
                            .param("status", "OPEN")
                            .param("skill", "Java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].status").value("OPEN"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("성공: 복수 스킬 검색 (Java + Spring) → 200 OK")
        void searchProjects_MultipleSkills_Success() throws Exception {
            // given
            // skill 파라미터를 여러 개 전달하는 경우 (List<String> 바인딩)
            Pageable pageable = PageRequest.of(0, 20);
            given(projectService.searchProjects(
                    isNull(), isNull(), isNull(), eq(List.of("Java", "Spring")), any(Pageable.class))
            ).willReturn(new PageImpl<>(List.of(projectDto), pageable, 1));

            // when & then
            mockMvc.perform(get("/v1/projects/search")
                            .param("skill", "Java", "Spring"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("성공: 조건 불일치 시 빈 페이지 반환 → 200 OK")
        void searchProjects_NoMatch_EmptyPage() throws Exception {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            given(projectService.searchProjects(
                    eq("없는프로젝트"), isNull(), isNull(), isNull(), any(Pageable.class))
            ).willReturn(new PageImpl<>(List.of(), pageable, 0));

            // when & then
            mockMvc.perform(get("/v1/projects/search")
                            .param("keyword", "없는프로젝트"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("프로젝트 생성 - 필수 필드 누락 (400)")
    class CreateProjectValidation {

        @Test
        @DisplayName("실패: title 누락 → 400 Bad Request")
        void createProject_BlankTitle_Fail() throws Exception {
            // given
            // title 필드를 빈 문자열로 전달 → @NotBlank 위반
            ProjectCreateRequestDto request = new ProjectCreateRequestDto(
                    "", "Test Description", 1L, 100L, 500L, 30, List.of("Java"), 10
            );

            // when & then
            mockMvc.perform(post("/v1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.data.title").value("제목은 필수입니다."));
        }

        @Test
        @DisplayName("실패: description 누락 → 400 Bad Request")
        void createProject_BlankDescription_Fail() throws Exception {
            // given
            // description 필드를 빈 문자열로 전달 → @NotBlank 위반
            ProjectCreateRequestDto request = new ProjectCreateRequestDto(
                    "Test Project", "", 1L, 100L, 500L, 30, List.of("Java"), 10
            );

            // when & then
            mockMvc.perform(post("/v1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.data.description").value("설명은 필수입니다."));
        }

        @Test
        @DisplayName("실패: categoryId null → 400 Bad Request")
        void createProject_NullCategoryId_Fail() throws Exception {
            // given
            // categoryId를 null로 전달 → @NotNull 위반
            ProjectCreateRequestDto request = new ProjectCreateRequestDto(
                    "Test Project", "Test Description", null, 100L, 500L, 30, List.of("Java"), 10
            );

            // when & then
            mockMvc.perform(post("/v1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.data.categoryId").value("카테고리는 필수입니다."));
        }

        @Test
        @DisplayName("실패: skills 빈 리스트 → 400 Bad Request")
        void createProject_EmptySkills_Fail() throws Exception {
            // given
            // skills를 빈 리스트로 전달 → @NotEmpty 위반
            ProjectCreateRequestDto request = new ProjectCreateRequestDto(
                    "Test Project", "Test Description", 1L, 100L, 500L, 30, List.of(), 10
            );

            // when & then
            mockMvc.perform(post("/v1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.data.skills").value("기술은 필수입니다."));
        }

        @Test
        @DisplayName("실패: maxProposalCount null → 400 Bad Request")
        void createProject_NullMaxProposalCount_Fail() throws Exception {
            // given
            // maxProposalCount를 null로 전달 → @NotNull 위반
            ProjectCreateRequestDto request = new ProjectCreateRequestDto(
                    "Test Project", "Test Description", 1L, 100L, 500L, 30, List.of("Java"), null
            );

            // when & then
            mockMvc.perform(post("/v1/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.data.maxProposalCount").value("최대 지원자 수는 필수입니다."));
        }
    }

    @Nested
    @DisplayName("타인 프로젝트 수정·삭제 - 403 권한 없음")
    class UnauthorizedAccess {

        @Test
        @DisplayName("실패: 타인 프로젝트 수정 시도 → 403 Forbidden")
        void updateProject_ByOtherClient_Forbidden() throws Exception {
            // given
            ProjectUpdateRequestDto request = new ProjectUpdateRequestDto(
                    "Hacked Title", "Hacked Desc", 1L, 100L, 500L, 30, List.of("Java"), 10
            );
            // AOP가 던질 예외를 Service Mock에서 재현
            given(projectService.updateProject(eq(1L), any(ProjectUpdateRequestDto.class)))
                    .willThrow(new ProjectException(ErrorCode.USER_FORBIDDEN));

            // when & then
            mockMvc.perform(put("/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_FORBIDDEN.getMessage()));
        }

        @Test
        @DisplayName("실패: 타인 프로젝트 삭제 시도 → 403 Forbidden")
        void deleteProject_ByOtherClient_Forbidden() throws Exception {
            // given
            // void 반환 메서드이므로 doThrow 방식 사용
            doThrow(new ProjectException(ErrorCode.USER_FORBIDDEN))
                    .when(projectService).deleteProject(1L);

            // when & then
            mockMvc.perform(delete("/v1/projects/1"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.USER_FORBIDDEN.getMessage()));
        }
    }
}
