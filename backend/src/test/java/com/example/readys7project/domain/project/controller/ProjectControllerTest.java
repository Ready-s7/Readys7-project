package com.example.readys7project.domain.project.controller;

import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.project.dto.request.ProjectCreateRequestDto;
import com.example.readys7project.domain.project.dto.request.ProjectStatusUpdateRequestDto;
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
import static org.mockito.Mockito.doNothing;
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
}
