package com.example.readys7project.domain.search.controller;

import com.example.readys7project.domain.search.dto.response.GlobalSearchResponseDto;
import com.example.readys7project.domain.search.service.SearchService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private SearchController searchController;

    @Mock
    private SearchService searchService;

    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("test@test.com")
                .name("Test User")
                .userRole(UserRole.DEVELOPER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        customUserDetails = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
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
    @DisplayName("통합 검색 V1 호출 성공")
    void searchV1_Success() throws Exception {
        // given
        GlobalSearchResponseDto emptyResponse = SearchService.empty(Pageable.unpaged());
        given(searchService.searchV1(anyLong(), anyString(), any(Pageable.class))).willReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/v1/search")
                        .param("keyword", "java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("인기 검색어 조회 성공")
    void getPopularRanking_Success() throws Exception {
        // given
        given(searchService.getPopularRanking(anyInt())).willReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(get("/v1/search/popular")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("통합 검색 V2 호출 성공")
    void searchV2_Success() throws Exception {
        // given
        GlobalSearchResponseDto emptyResponse = SearchService.empty(Pageable.unpaged());
        given(searchService.searchV2(anyLong(), anyString(), any(Pageable.class))).willReturn(emptyResponse);

        // when & then
        mockMvc.perform(get("/v2/search")
                        .param("keyword", "spring")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
