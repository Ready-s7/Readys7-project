package com.example.readys7project.domain.chat.message.service;

import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageCursorResponseDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageResponseDto;
import com.example.readys7project.domain.chat.message.entity.Message;
import com.example.readys7project.domain.chat.message.event.MessageCreatedEvent;
import com.example.readys7project.domain.chat.message.repository.MessageRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.MessageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @InjectMocks
    private MessageService messageService;

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UnreadCountService unreadCountService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("메시지 저장 성공 및 이벤트 발행 검증")
    void saveMessageSuccess() {
        // given
        String email = "client@test.com";
        Long roomId = 1L;
        SendMessageRequestDto request = new SendMessageRequestDto("Hello");
        
        User clientUser = createTestUser(1L, email, UserRole.CLIENT);
        User devUser = createTestUser(2L, "dev@test.com", UserRole.DEVELOPER);
        Client client = createTestClient(1L, clientUser);
        Developer developer = createTestDeveloper(1L, devUser);
        ChatRoom chatRoom = createTestChatRoom(roomId, client, developer);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(clientUser));
        given(chatRoomRepository.findById(roomId)).willReturn(Optional.of(chatRoom));
        
        Message message = Message.builder().chatRoom(chatRoom).user(clientUser).content(request.content()).build();
        ReflectionTestUtils.setField(message, "id", 1L);
        given(messageRepository.save(any(Message.class))).willReturn(message);

        // when
        MessageResponseDto result = messageService.saveMessage(roomId, request, email);

        // then
        assertThat(result.content()).isEqualTo("Hello");
        verify(eventPublisher).publishEvent(any(MessageCreatedEvent.class));
    }

    @Test
    @DisplayName("메시지 저장 실패 - 참여자 아님")
    void saveMessageFail_NotParticipant() {
        // given
        String email = "other@test.com";
        User otherUser = createTestUser(3L, email, UserRole.CLIENT);
        
        User clientUser = createTestUser(1L, "client@test.com", UserRole.CLIENT);
        User devUser = createTestUser(2L, "dev@test.com", UserRole.DEVELOPER);
        ChatRoom chatRoom = createTestChatRoom(1L, createTestClient(1L, clientUser), createTestDeveloper(1L, devUser));

        given(userRepository.findByEmail(email)).willReturn(Optional.of(otherUser));
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));

        // when & then
        assertThatThrownBy(() -> messageService.saveMessage(1L, new SendMessageRequestDto("Hi"), email))
                .isInstanceOf(MessageException.class)
                .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("시스템 메시지 저장 - 입장 시 unread count 초기화")
    void saveSystemMessageEnter() {
        // given
        String email = "client@test.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        ChatRoom chatRoom = createTestChatRoom(1L, createTestClient(1L, user), createTestDeveloper(1L, createTestUser(2L, "dev@test.com", UserRole.DEVELOPER)));

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
        
        Message message = Message.builder().chatRoom(chatRoom).user(user).content(user.getName() + "님이 입장했습니다.").isSystem(true).build();
        given(messageRepository.save(any(Message.class))).willReturn(message);

        // when
        messageService.saveSystemMessage(1L, email, true);

        // then
        verify(unreadCountService).reset(1L, 1L);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("시스템 메시지 저장 - 퇴장 시 unread count 초기화 안 함")
    void saveSystemMessageLeave() {
        // given
        String email = "client@test.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        ChatRoom chatRoom = createTestChatRoom(1L, createTestClient(1L, user), createTestDeveloper(1L, createTestUser(2L, "dev@test.com", UserRole.DEVELOPER)));

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));

        Message message = Message.builder().chatRoom(chatRoom).user(user).content(user.getName() + "님이 퇴장했습니다.").isSystem(true).build();
        given(messageRepository.save(any(Message.class))).willReturn(message);

        // when
        messageService.saveSystemMessage(1L, email, false);

        // then
        verify(unreadCountService, never()).reset(anyLong(), anyLong());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("이전 메시지 조회 - 커서 페이징")
    void getMessagesSuccess() {
        // given
        String email = "client@test.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        ChatRoom chatRoom = createTestChatRoom(1L, createTestClient(1L, user), createTestDeveloper(1L, createTestUser(2L, "dev@test.com", UserRole.DEVELOPER)));
        
        List<Message> messages = new ArrayList<>();
        for (long i = 1; i <= 11; i++) {
            Message m = Message.builder().chatRoom(chatRoom).user(user).content("msg " + i).build();
            ReflectionTestUtils.setField(m, "id", i);
            messages.add(m);
        }

        PageRequest pageable = PageRequest.of(0, 10);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(chatRoom));
        given(messageRepository.findMessages(1L, null, pageable)).willReturn(messages);

        // when
        MessageCursorResponseDto result = messageService.getMessages(1L, null, pageable, email);

        // then
        assertThat(result.messages()).hasSize(10);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(10L);
    }

    @Test
    @DisplayName("메시지 수정 성공")
    void updateMessageSuccess() {
        // given
        String email = "client@test.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        ChatRoom chatRoom = createTestChatRoom(1L, createTestClient(1L, user), createTestDeveloper(1L, createTestUser(2L, "dev@test.com", UserRole.DEVELOPER)));
        Message message = Message.builder().chatRoom(chatRoom).user(user).content("old").build();
        ReflectionTestUtils.setField(message, "id", 1L);

        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        MessageResponseDto result = messageService.updateMessage(1L, "new", email);

        // then
        assertThat(message.getContent()).isEqualTo("new");
        assertThat(result.content()).isEqualTo("new");
    }

    @Test
    @DisplayName("메시지 수정 실패 - 작성자 아님")
    void updateMessageFail_NotAuthor() {
        // given
        String email = "other@test.com";
        User user = createTestUser(1L, "client@test.com", UserRole.CLIENT);
        ChatRoom chatRoom = createTestChatRoom(1L, createTestClient(1L, user), createTestDeveloper(1L, createTestUser(2L, "dev@test.com", UserRole.DEVELOPER)));
        Message message = Message.builder().chatRoom(chatRoom).user(user).content("old").build();

        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when & then
        assertThatThrownBy(() -> messageService.updateMessage(1L, "new", email))
                .isInstanceOf(MessageException.class)
                .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("메시지 삭제 성공")
    void deleteMessageSuccess() {
        // given
        String email = "client@test.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        ChatRoom chatRoom = createTestChatRoom(1L, createTestClient(1L, user), createTestDeveloper(1L, createTestUser(2L, "dev@test.com", UserRole.DEVELOPER)));
        Message message = Message.builder().chatRoom(chatRoom).user(user).content("del").build();

        given(messageRepository.findById(1L)).willReturn(Optional.of(message));

        // when
        messageService.deleteMessage(1L, email);

        // then
        verify(messageRepository).deleteById(1L);
    }

    // Helper methods
    private User createTestUser(Long id, String email, UserRole role) {
        User user = User.builder().email(email).name("User " + id).userRole(role).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Client createTestClient(Long id, User user) {
        Client client = Client.builder().user(user).title("Client " + id).build();
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }

    private Developer createTestDeveloper(Long id, User user) {
        Developer developer = Developer.builder().user(user).title("Developer " + id).build();
        ReflectionTestUtils.setField(developer, "id", id);
        return developer;
    }

    private ChatRoom createTestChatRoom(Long id, Client client, Developer developer) {
        ChatRoom chatRoom = ChatRoom.builder().client(client).developer(developer).build();
        ReflectionTestUtils.setField(chatRoom, "id", id);
        return chatRoom;
    }
}
