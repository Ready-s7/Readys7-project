package com.example.readys7project.domain.skill.controller;

import com.example.readys7project.domain.skill.dto.request.CreateSkillRequestDto;
import com.example.readys7project.domain.skill.dto.request.UpdateSkillRequestDto;
import com.example.readys7project.domain.skill.dto.response.SkillResponseDto;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.skill.service.SkillService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.SkillException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
class SkillControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private SkillController skillController;

    @Mock
    private SkillService skillService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(skillController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                User user = User.builder().email("admin@test.com").build();
                                ReflectionTestUtils.setField(user, "id", 1L);
                                return new CustomUserDetails(user);
                            }
                        }
                )
                .build();
    }

    private SkillResponseDto createSkillResponseDto(Long id, String name, SkillCategory category) {
        return SkillResponseDto.builder()
                .id(id)
                .adminId(1L)
                .adminName("관리자")
                .name(name)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("기술 생성 API 테스트")
    class CreateSkill {
        @Test
        @DisplayName("성공: 기술을 생성하고 201 상태코드를 반환한다")
        void createSkill_Success() throws Exception {
            // given
            CreateSkillRequestDto request = new CreateSkillRequestDto("Java", SkillCategory.BACKEND);
            SkillResponseDto response = createSkillResponseDto(1L, "Java", SkillCategory.BACKEND);

            given(skillService.createSkill(any(CreateSkillRequestDto.class), anyString())).willReturn(response);

            // when & then
            mockMvc.perform(post("/v1/skills")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Java"));
        }
    }

    @Nested
    @DisplayName("기술 조회 API 테스트")
    class GetSkills {
        @Test
        @DisplayName("성공: 기술 목록을 조회하고 200 상태코드를 반환한다")
        void getSkills_Success() throws Exception {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            SkillResponseDto response = createSkillResponseDto(1L, "Java", SkillCategory.BACKEND);
            Page<SkillResponseDto> page = new PageImpl<>(List.of(response), pageRequest, 1);

            given(skillService.getSkills(any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/v1/skills")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("Java"));
        }
    }

    @Nested
    @DisplayName("기술 검색 API 테스트")
    class SearchSkills {
        @Test
        @DisplayName("성공: 조건에 맞는 기술을 검색하고 200 상태코드를 반환한다")
        void searchSkills_Success() throws Exception {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            SkillResponseDto response = createSkillResponseDto(1L, "Java", SkillCategory.BACKEND);
            Page<SkillResponseDto> page = new PageImpl<>(List.of(response), pageRequest, 1);

            given(skillService.searchSkills(anyString(), any(SkillCategory.class), any())).willReturn(page);

            // when & then
            mockMvc.perform(get("/v1/skills/search")
                            .param("name", "Java")
                            .param("category", "BACKEND")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].name").value("Java"));
        }
    }

    @Nested
    @DisplayName("기술 수정 API 테스트")
    class UpdateSkill {
        @Test
        @DisplayName("성공: 기술 정보를 수정하고 200 상태코드를 반환한다")
        void updateSkill_Success() throws Exception {
            // given
            Long skillId = 1L;
            UpdateSkillRequestDto request = new UpdateSkillRequestDto("Python", SkillCategory.BACKEND);
            SkillResponseDto response = createSkillResponseDto(skillId, "Python", SkillCategory.BACKEND);

            given(skillService.updateSkill(eq(skillId), any(UpdateSkillRequestDto.class))).willReturn(response);

            // when & then
            mockMvc.perform(patch("/v1/skills/{skillId}", skillId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Python"));
        }

        @Test
        @DisplayName("실패: 기술이 존재하지 않으면 404 상태코드를 반환한다")
        void updateSkill_Fail_NotFound() throws Exception {
            // given
            Long skillId = 99L;
            UpdateSkillRequestDto request = new UpdateSkillRequestDto("Python", SkillCategory.BACKEND);

            given(skillService.updateSkill(eq(skillId), any(UpdateSkillRequestDto.class)))
                    .willThrow(new SkillException(ErrorCode.SKILL_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/v1/skills/{skillId}", skillId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SKILL_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("기술 삭제 API 테스트")
    class DeleteSkill {
        @Test
        @DisplayName("성공: 기술을 삭제하고 204 상태코드를 반환한다")
        void deleteSkill_Success() throws Exception {
            // given
            Long skillId = 1L;

            // when & then
            mockMvc.perform(delete("/v1/skills/{skillId}", skillId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("실패: 삭제할 기술이 존재하지 않으면 404 상태코드를 반환한다")
        void deleteSkill_Fail_NotFound() throws Exception {
            // given
            Long skillId = 99L;
            doThrow(new SkillException(ErrorCode.SKILL_NOT_FOUND)).when(skillService).deleteSkill(skillId);

            // when & then
            mockMvc.perform(delete("/v1/skills/{skillId}", skillId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("SKILL_NOT_FOUND"));
        }
    }
}
