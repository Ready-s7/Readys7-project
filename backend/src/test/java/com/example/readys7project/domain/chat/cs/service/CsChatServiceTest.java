package com.example.readys7project.domain.chat.cs.service;

import com.example.readys7project.domain.chat.cs.dto.request.CsChatRoomRequestDto;
import com.example.readys7project.domain.chat.cs.dto.response.CsChatRoomResponseDto;
import com.example.readys7project.domain.chat.cs.dto.response.CsMessageResponseDto;
import com.example.readys7project.domain.chat.cs.entity.CsChatRoom;
import com.example.readys7project.domain.chat.cs.entity.CsMessage;
import com.example.readys7project.domain.chat.cs.enums.CsStatus;
import com.example.readys7project.domain.chat.cs.repository.CsChatRoomRepository;
import com.example.readys7project.domain.chat.cs.repository.CsMessageRepository;
import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
import com.example.readys7project.domain.chat.message.enums.MessageEventType;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.domain.ChatRoomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsChatServiceTest {

    @InjectMocks
    private CsChatService csChatService;

    @Mock
    private CsChatRoomRepository csChatRoomRepository;

    @Mock
    private CsMessageRepository csMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("CS 문의방 생성 성공")
    void createCsRoom_Success() {
        // given
        String email = "user@test.com";
        User user = createUser(1L, email, UserRole.CLIENT);
        CsChatRoomRequestDto request = new CsChatRoomRequestDto("문의합니다");
        
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(csChatRoomRepository.save(any(CsChatRoom.class))).willAnswer(invocation -> {
            CsChatRoom room = invocation.getArgument(0);
            ReflectionTestUtils.setField(room, "id", 1L);
            return room;
        });

        // when
        CsChatRoomResponseDto result = csChatService.createCsRoom(request, email);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("문의합니다");
        verify(csChatRoomRepository).save(any(CsChatRoom.class));
    }

    @Test
    @DisplayName("문의방 상세 조회 - 권한 없는 사용자 접근 시 예외 발생")
    void getCsRoom_Forbidden() {
        // given
        Long roomId = 1L;
        String userEmail = "other@test.com";
        User inquirer = createUser(1L, "inquirer@test.com", UserRole.CLIENT);
        User otherUser = createUser(2L, userEmail, UserRole.CLIENT);
        CsChatRoom room = createChatRoom(roomId, inquirer);

        given(csChatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.findByEmail(userEmail)).willReturn(Optional.of(otherUser));

        // when & then
        assertThatThrownBy(() -> csChatService.getCsRoom(roomId, userEmail))
                .isInstanceOf(ChatRoomException.class);
    }

    @Test
    @DisplayName("문의방 상세 조회 - 관리자는 접근 가능")
    void getCsRoom_AdminSuccess() {
        // given
        Long roomId = 1L;
        String adminEmail = "admin@test.com";
        User inquirer = createUser(1L, "inquirer@test.com", UserRole.CLIENT);
        User admin = createUser(2L, adminEmail, UserRole.ADMIN);
        CsChatRoom room = createChatRoom(roomId, inquirer);

        given(csChatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.findByEmail(adminEmail)).willReturn(Optional.of(admin));

        // when
        CsChatRoomResponseDto result = csChatService.getCsRoom(roomId, adminEmail);

        // then
        assertThat(result.id()).isEqualTo(roomId);
    }

    @Test
    @DisplayName("시스템 메시지 저장 - 입장")
    void saveSystemMessage_Enter() {
        // given
        Long roomId = 1L;
        String email = "user@test.com";
        User user = createUser(1L, email, UserRole.CLIENT);
        CsChatRoom room = createChatRoom(roomId, user);

        given(csChatRoomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(csMessageRepository.save(any(CsMessage.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        CsMessageResponseDto result = csChatService.saveSystemMessage(roomId, email, true);

        // then
        assertThat(result.content()).contains("입장하셨습니다");
        assertThat(result.eventType()).isEqualTo(MessageEventType.ENTER);
    }

    @Test
    @DisplayName("문의 상태 변경 - 관리자 기능")
    void updateCsStatus_Success() {
        // given
        Long roomId = 1L;
        User inquirer = createUser(1L, "user@test.com", UserRole.CLIENT);
        CsChatRoom room = createChatRoom(roomId, inquirer);
        assertThat(room.getStatus()).isEqualTo(CsStatus.WAITING);

        given(csChatRoomRepository.findById(roomId)).willReturn(Optional.of(room));

        // when
        CsChatRoomResponseDto result = csChatService.updateCsStatus(roomId, CsStatus.COMPLETED);

        // then
        assertThat(result.status()).isEqualTo(CsStatus.COMPLETED);
    }

    @Test
    @DisplayName("메시지 수정 - 타인의 메시지 수정 시 예외 발생")
    void updateMessage_Forbidden() {
        // given
        Long messageId = 1L;
        String email = "other@test.com";
        User sender = createUser(1L, "sender@test.com", UserRole.CLIENT);
        User otherUser = createUser(2L, email, UserRole.CLIENT);
        CsMessage message = createMessage(messageId, sender, "Original content");

        given(csMessageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(otherUser));

        SendMessageRequestDto request = new SendMessageRequestDto("New content");

        // when & then
        assertThatThrownBy(() -> csChatService.updateMessage(messageId, request, email))
                .isInstanceOf(ChatRoomException.class);
    }

    @Test
    @DisplayName("메시지 삭제 - 타인의 메시지 삭제 시 예외 발생")
    void deleteMessage_Forbidden() {
        // given
        Long messageId = 1L;
        String email = "other@test.com";
        User sender = createUser(1L, "sender@test.com", UserRole.CLIENT);
        User otherUser = createUser(2L, email, UserRole.CLIENT);
        CsMessage message = createMessage(messageId, sender, "Content to delete");

        given(csMessageRepository.findById(messageId)).willReturn(Optional.of(message));
        given(userRepository.findByEmail(email)).willReturn(Optional.of(otherUser));

        // when & then
        assertThatThrownBy(() -> csChatService.deleteMessage(messageId, email))
                .isInstanceOf(ChatRoomException.class);
    }

    @Test
    @DisplayName("메시지 수정 - 존재하지 않는 메시지 수정 시 예외 발생")
    void updateMessage_NotFound() {
        // given
        Long messageId = 99L;
        String email = "user@test.com";
        given(csMessageRepository.findById(messageId)).willReturn(Optional.empty());

        SendMessageRequestDto request = new SendMessageRequestDto("New content");

        // when & then
        assertThatThrownBy(() -> csChatService.updateMessage(messageId, request, email))
                .isInstanceOf(ChatRoomException.class);
    }

    private User createUser(Long id, String email, UserRole role) {
        User user = User.builder()
                .email(email)
                .name("Test User")
                .userRole(role)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private CsChatRoom createChatRoom(Long id, User inquirer) {
        CsChatRoom room = CsChatRoom.builder()
                .inquirer(inquirer)
                .title("Test Title")
                .build();
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "status", CsStatus.WAITING);
        return room;
    }

    private CsMessage createMessage(Long id, User sender, String content) {
        CsMessage message = CsMessage.builder()
                .sender(sender)
                .content(content)
                .eventType(MessageEventType.SEND)
                .build();
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
