package com.example.readys7project.domain.chat.message.controller;

import com.example.readys7project.domain.chat.message.dto.request.UpdateMessageRequestDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageCursorResponseDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageResponseDto;
import com.example.readys7project.domain.chat.message.enums.MessageEventType;
import com.example.readys7project.domain.chat.message.publisher.RedisMessagePublisher;
import com.example.readys7project.domain.chat.message.service.MessageService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.MessageException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private MessageController messageController;

    @Mock
    private MessageService messageService;

    @Mock
    private RedisMessagePublisher redisMessagePublisher;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController)
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
    @DisplayName("메시지 조회 API 성공")
    void getMessagesApiSuccess() throws Exception {
        // given
        MessageResponseDto message = MessageResponseDto.builder()
                .id(1L).chatRoomId(1L).senderId(1L).senderName("Tester").content("Hello")
                .eventType(MessageEventType.SEND).isRead(false).isSystem(false).sentAt(LocalDateTime.now()).build();
        MessageCursorResponseDto response = MessageCursorResponseDto.builder()
                .messages(List.of(message)).hasNext(false).nextCursor(null).build();

        given(messageService.getMessages(anyLong(), any(), any(), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(get("/v1/chat/rooms/1/messages")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messages[0].content").value("Hello"));
    }

    @Test
    @DisplayName("메시지 수정 API 성공")
    void updateMessageApiSuccess() throws Exception {
        // given
        UpdateMessageRequestDto request = new UpdateMessageRequestDto("Updated content");
        MessageResponseDto response = MessageResponseDto.builder()
                .id(1L).chatRoomId(1L).senderId(1L).senderName("Tester").content("Updated content")
                .eventType(MessageEventType.EDIT).isRead(false).isSystem(false).sentAt(LocalDateTime.now()).build();

        given(messageService.updateMessage(anyLong(), anyString(), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/v1/chat/messages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("메시지 수정 API 실패 - 작성자 아님")
    void updateMessageApiFail_Forbidden() throws Exception {
        // given
        given(messageService.updateMessage(anyLong(), anyString(), anyString()))
                .willThrow(new MessageException(ErrorCode.USER_FORBIDDEN));

        // when & then
        mockMvc.perform(patch("/v1/chat/messages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMessageRequestDto("new"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"));
    }

    @Test
    @DisplayName("메시지 삭제 API 성공")
    void deleteMessageApiSuccess() throws Exception {
        // given
        MessageResponseDto response = MessageResponseDto.builder()
                .id(1L).chatRoomId(1L).senderId(1L).senderName("Tester").content("deleted")
                .eventType(MessageEventType.DELETE).isRead(false).isSystem(false).sentAt(LocalDateTime.now()).build();

        given(messageService.deleteMessage(anyLong(), anyString())).willReturn(response);

        // when & then
        mockMvc.perform(delete("/v1/chat/messages/1"))
                .andExpect(status().isNoContent());
    }
}
