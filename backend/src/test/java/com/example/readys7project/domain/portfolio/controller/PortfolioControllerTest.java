package com.example.readys7project.domain.portfolio.controller;

import com.example.readys7project.domain.portfolio.dto.PortfolioDto;
import com.example.readys7project.domain.portfolio.dto.request.PortfolioRequestDto;
import com.example.readys7project.domain.portfolio.dto.request.PortfolioUpdateRequestDto;
import com.example.readys7project.domain.portfolio.service.PortfolioService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.PortfolioException;
import com.example.readys7project.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private PortfolioController portfolioController;

    @Mock
    private PortfolioService portfolioService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CustomUserDetails customUserDetails;
    private PortfolioDto portfolioDto;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());

        User user = User.builder()
                .email("developer@example.com")
                .name("Test Developer")
                .userRole(UserRole.DEVELOPER)
                .build();
        customUserDetails = new CustomUserDetails(user);

        portfolioDto = PortfolioDto.builder()
                .id(1L)
                .developerId(1L)
                .title("Test Portfolio")
                .description("Test Description")
                .skills(List.of("Java"))
                .createdAt(LocalDateTime.now())
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(portfolioController)
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
    @DisplayName("성공: 포트폴리오 생성 API")
    void createPortfolio_Success() throws Exception {
        // given
        PortfolioRequestDto request = PortfolioRequestDto.builder()
                .title("Test Portfolio")
                .description("Test Description")
                .skills(List.of("Java"))
                .build();
        given(portfolioService.createPortfolio(any(PortfolioRequestDto.class), anyString())).willReturn(portfolioDto);

        // when & then
        mockMvc.perform(post("/v1/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Controller returns ok() with ApiResponseDto
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value(portfolioDto.title()));
    }

    @Test
    @DisplayName("성공: 포트폴리오 수정 API")
    void updatePortfolio_Success() throws Exception {
        // given
        PortfolioUpdateRequestDto request = PortfolioUpdateRequestDto.builder()
                .title("Updated Title")
                .build();
        given(portfolioService.updatePortfolio(eq(1L), any(PortfolioUpdateRequestDto.class), anyString())).willReturn(portfolioDto);

        // when & then
        mockMvc.perform(patch("/v1/portfolios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("성공: 포트폴리오 삭제 API")
    void deletePortfolio_Success() throws Exception {
        // given
        doNothing().when(portfolioService).deletePortfolio(eq(1L), anyString());

        // when & then
        mockMvc.perform(delete("/v1/portfolios/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("성공: 특정 개발자 포트폴리오 조회 API")
    void getPortfolio_Success() throws Exception {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageImpl<PortfolioDto> page = new PageImpl<>(List.of(portfolioDto), pageRequest, 1);
        given(portfolioService.getPortfolio(eq(1L), anyString(), anyInt(), anyInt())).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/portfolios")
                        .param("developerId", "1")
                        .param("skill", "Java")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value(portfolioDto.title()));
    }

    @Test
    @DisplayName("성공: 포트폴리오 검색 API")
    void searchPortfolios_Success() throws Exception {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageImpl<PortfolioDto> page = new PageImpl<>(List.of(portfolioDto), pageRequest, 1);
        given(portfolioService.searchPortfolios(anyString(), anyInt(), anyInt())).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/portfolios/search")
                        .param("skill", "Java")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value(portfolioDto.title()));
    }

    @Test
    @DisplayName("실패: 포트폴리오 조회 - 존재하지 않는 포트폴리오")
    void getPortfolio_NotFound() throws Exception {
        // given
        given(portfolioService.getPortfolio(anyLong(), any(), anyInt(), anyInt()))
                .willThrow(new PortfolioException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/v1/portfolios")
                        .param("developerId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PORTFOLIO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(ErrorCode.PORTFOLIO_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.success").doesNotExist());
    }


    // 실패 시나리오.
    @Nested
    @DisplayName("포트폴리오 생성 API 추가 테스트")
    class CreatePortfolioAdditionalApiTest {

        @Test
        @DisplayName("실패: description이 비어 있으면 400 Bad Request를 반환한다")
        void createPortfolio_description누락_400응답() throws Exception {
            // given
            Map<String, Object> request = Map.of(
                    "title", "포트폴리오 제목",
                    "description", "",
                    "skills", List.of("Java")
            );
            // when
            ResultActions result = mockMvc.perform(post("/v1/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.data.description").exists());
        }

        @Test
        @DisplayName("실패: skills 필드가 누락되면 400 Bad Request를 반환한다")
        void createPortfolio_skills누락_400응답() throws Exception {
            // given
            Map<String, Object> request = Map.of(
                    "title", "포트폴리오 제목",
                    "description", "포트폴리오 설명"
            );
            // when
            ResultActions result = mockMvc.perform(post("/v1/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
            // then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.data.skills").exists());
        }

        @Test
        @DisplayName("실패: 클라이언트 권한 사용자가 생성 시도하면 403 Forbidden을 반환한다")
        void createPortfolio_클라이언트권한_403응답() throws Exception {
            // given
            given(portfolioService.createPortfolio(any(PortfolioRequestDto.class), eq("developer@example.com")))
                    .willThrow(new PortfolioException(ErrorCode.USER_FORBIDDEN));

            Map<String, Object> request = Map.of(
                    "title", "포트폴리오 제목",
                    "description", "포트폴리오 설명",
                    "skills", List.of("Java")
            );

            // when
            ResultActions result = mockMvc.perform(post("/v1/portfolios")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
        }
    }

}
