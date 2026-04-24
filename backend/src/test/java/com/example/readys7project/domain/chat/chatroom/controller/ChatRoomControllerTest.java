package com.example.readys7project.domain.chat.chatroom.controller;

import com.example.readys7project.domain.chat.chatroom.dto.request.CreateChatRoomRequestDto;
import com.example.readys7project.domain.chat.chatroom.dto.response.ChatRoomResponseDto;
import com.example.readys7project.domain.chat.chatroom.service.ChatRoomService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.ChatRoomException;
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
class ChatRoomControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private ChatRoomController chatRoomController;

    @Mock
    private ChatRoomService chatRoomService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatRoomController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(CustomUserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        User user = User.builder().email("test@test.com").name("Tester").userRole(UserRole.CLIENT).build();
                        ReflectionTestUtils.setField(user, "id", 1L);
                        return new CustomUserDetails(user);
                    }
                }, new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("채팅방 생성 API 성공")
    void createChatRoomApiSuccess() throws Exception {
        // given
        CreateChatRoomRequestDto request = new CreateChatRoomRequestDto(1L, 1L);
        ChatRoomResponseDto response = ChatRoomResponseDto.builder()
                .id(1L).projectId(1L).projectTitle("Title").clientId(1L).clientName("Client").developerId(1L).developerName("Dev")
                .unreadCount(0L).createdAt(LocalDateTime.now()).build();

        given(chatRoomService.createChatRoom(any(), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(post("/v1/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectTitle").value("Title"));
    }

    @Test
    @DisplayName("채팅방 생성 API 실패 - 권한 없음")
    void createChatRoomApiFail_Forbidden() throws Exception {
        // given
        given(chatRoomService.createChatRoom(any(), anyString()))
                .willThrow(new ChatRoomException(ErrorCode.USER_FORBIDDEN));

        // when & then
        mockMvc.perform(post("/v1/chat/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateChatRoomRequestDto(1L, 1L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_FORBIDDEN.getMessage()));
    }

    @Test
    @DisplayName("내 채팅방 목록 조회 API 성공")
    void getMyChatRoomsApiSuccess() throws Exception {
        // given
        ChatRoomResponseDto response = ChatRoomResponseDto.builder()
                .id(1L).projectId(1L).projectTitle("Title").clientId(1L).clientName("Client").developerId(1L).developerName("Dev")
                .unreadCount(0L).createdAt(LocalDateTime.now()).build();
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageImpl<ChatRoomResponseDto> page = new PageImpl<>(List.of(response), pageRequest, 1);

        given(chatRoomService.getMyChatRooms(anyString(), any())).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/chat/rooms")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].projectTitle").value("Title"));
    }

    @Test
    @DisplayName("채팅방 삭제 API 성공")
    void deleteChatRoomApiSuccess() throws Exception {
        // when & then
        mockMvc.perform(delete("/v1/chat/rooms/1"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("채팅방 삭제 API 실패 - 채팅방 없음")
    void deleteChatRoomApiFail_NotFound() throws Exception {
        // given
        doThrow(new ChatRoomException(ErrorCode.CHATROOM_NOT_FOUND))
                .when(chatRoomService).deleteChatRoom(anyLong(), anyString());

        // when & then
        mockMvc.perform(delete("/v1/chat/rooms/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHATROOM_NOT_FOUND"));
    }
}
