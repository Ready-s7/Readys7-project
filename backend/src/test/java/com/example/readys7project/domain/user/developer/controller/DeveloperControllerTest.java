package com.example.readys7project.domain.user.developer.controller;

import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.developer.dto.DeveloperDto;
import com.example.readys7project.domain.user.developer.dto.request.DeveloperProfileRequestDto;
import com.example.readys7project.domain.user.developer.service.DeveloperService;
import com.example.readys7project.domain.user.enums.ParticipateType;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.DeveloperException;
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
import org.springframework.data.domain.Page;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeveloperControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private DeveloperController developerController;

    @Mock
    private DeveloperService developerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("dev@test.com")
                .name("Test User")
                .userRole(UserRole.DEVELOPER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        customUserDetails = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders.standaloneSetup(developerController)
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
    @DisplayName("전체 개발자 목록 조회 성공")
    void getAllDevelopers_Success() throws Exception {
        // given
        DeveloperDto dto = createDeveloperDto(1L);
        Page<DeveloperDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        given(developerService.getAllDevelopers(any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/developers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Test User"));
    }

    @Test
    @DisplayName("내 프로젝트 목록 조회 성공")
    void getMyProjects_Success() throws Exception {
        // given
        ProjectResponseDto dto = createProjectResponseDto(1L);
        Page<ProjectResponseDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        given(developerService.getMyProjects(anyString(), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/developers/me/my-projects")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Test Project"));
    }

    @Test
    @DisplayName("개발자 상세 조회 성공")
    void getDeveloperById_Success() throws Exception {
        // given
        DeveloperDto dto = createDeveloperDto(1L);
        given(developerService.getDeveloperById(1L)).willReturn(dto);

        // when & then
        mockMvc.perform(get("/v1/developers/{developerId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test User"));
    }

    @Test
    @DisplayName("개발자 상세 조회 실패 - 존재하지 않음")
    void getDeveloperById_NotFound() throws Exception {
        // given
        given(developerService.getDeveloperById(1L)).willThrow(new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/v1/developers/{developerId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEVELOPER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(ErrorCode.DEVELOPER_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("개발자 검색 성공")
    void searchDevelopers_Success() throws Exception {
        // given
        DeveloperDto dto = createDeveloperDto(1L);
        Page<DeveloperDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10), 1);
        given(developerService.searchDevelopers(anyList(), anyDouble(), any(Pageable.class))).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/developers/search")
                        .param("skills", "Java,Spring")
                        .param("minRating", "4.0")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @DisplayName("개발자 프로필 수정 성공")
    void updateProfile_Success() throws Exception {
        // given
        DeveloperProfileRequestDto request = new DeveloperProfileRequestDto(
                "Updated Title", List.of("Java"), 30000, 50000, "1시간", true
        );
        DeveloperDto response = createDeveloperDto(1L);
        given(developerService.updateProfile(any(DeveloperProfileRequestDto.class), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(put("/v1/developers/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    private DeveloperDto createDeveloperDto(Long id) {
        return DeveloperDto.builder()
                .id(id)
                .userId(1L)
                .name("Test User")
                .title("Fullstack Developer")
                .rating(4.5)
                .reviewCount(10)
                .completedProjects(5)
                .skills(List.of("Java", "Spring"))
                .minHourlyPay(30000)
                .maxHourlyPay(50000)
                .responseTime("1시간")
                .description("Test Description")
                .availableForWork(true)
                .participateType(ParticipateType.INDIVIDUAL)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ProjectResponseDto createProjectResponseDto(Long id) {
        return new ProjectResponseDto(
                id, 1L, 1L, "Test Project", "Description", "Web",
                1000000L, 5000000L, 30, List.of("Java"), "OPEN",
                0, 10, "Client Name", 4.5, LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
