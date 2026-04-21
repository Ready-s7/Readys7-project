package com.example.readys7project.domain.chat.cs.controller;

import com.example.readys7project.domain.chat.cs.dto.request.CsChatRoomRequestDto;
import com.example.readys7project.domain.chat.cs.dto.response.CsChatRoomResponseDto;
import com.example.readys7project.domain.chat.cs.service.CsChatService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CsChatControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private CsChatController csChatController;

    @Mock
    private CsChatService csChatService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CustomUserDetails customUserDetails;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("user@test.com")
                .name("Test User")
                .userRole(UserRole.CLIENT)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        customUserDetails = new CustomUserDetails(user);

        mockMvc = MockMvcBuilders.standaloneSetup(csChatController)
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
    @DisplayName("문의방 생성 요청 성공")
    void createCsRoom_Success() throws Exception {
        // given
        CsChatRoomRequestDto request = new CsChatRoomRequestDto("문의사항");
        CsChatRoomResponseDto response = CsChatRoomResponseDto.builder()
                .id(1L)
                .title("문의사항")
                .build();
        given(csChatService.createCsRoom(any(CsChatRoomRequestDto.class), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(post("/v1/cs/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.title").value("문의사항"));
    }

    @Test
    @DisplayName("문의방 생성 요청 실패 - 제목 누락")
    void createCsRoom_BadRequest() throws Exception {
        // given
        CsChatRoomRequestDto request = new CsChatRoomRequestDto(""); // 빈 제목

        // when & then
        mockMvc.perform(post("/v1/cs/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("내 문의 목록 조회 성공")
    void getMyCsRooms_Success() throws Exception {
        // given
        Page<CsChatRoomResponseDto> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        given(csChatService.getMyCsRooms(anyInt(), anyInt(), anyString())).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/cs/rooms")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("문의방 상세 조회 성공")
    void getCsRoom_Success() throws Exception {
        // given
        CsChatRoomResponseDto response = CsChatRoomResponseDto.builder().id(1L).build();
        given(csChatService.getCsRoom(anyLong(), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(get("/v1/cs/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("[관리자] 전체 문의 목록 조회 성공")
    void getAllCsRooms_AdminSuccess() throws Exception {
        // given
        Page<CsChatRoomResponseDto> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        given(csChatService.getAllCsRooms(any(), anyInt(), anyInt())).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/admin/cs/rooms")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
