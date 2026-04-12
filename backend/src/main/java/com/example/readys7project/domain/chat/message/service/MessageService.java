package com.example.readys7project.domain.chat.message.service;

import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageCursorResponseDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageResponseDto;
import com.example.readys7project.domain.chat.message.entity.Message;
import com.example.readys7project.domain.chat.message.repository.MessageRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.MessageException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final UnreadCountService unreadCountService;

    @Transactional
    public MessageResponseDto saveMessage(Long roomId, SendMessageRequestDto request, String email) {

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new MessageException(ErrorCode.USER_NOT_FOUND)
        );

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new MessageException(ErrorCode.CHATROOM_NOT_FOUND)
        );

        // 참여자 검증
        boolean isParticipant = chatRoom.getClient().getUser().getId().equals(user.getId())
                || chatRoom.getDeveloper().getUser().getId().equals(user.getId());

        if (!isParticipant) {
            throw new MessageException(ErrorCode.USER_FORBIDDEN);
        }

        Message message = Message.builder()
                .chatRoom(chatRoom)
                .user(user)
                .content(request.content())
                .build();

        Message savedMessage = messageRepository.save(message);

        // 상대방 unread count 증가
        Long receiverId = chatRoom.getClient().getUser().getId().equals(user.getId())
                ? chatRoom.getDeveloper().getUser().getId()  // 보낸 사람이 client면 developer가 수신자
                : chatRoom.getClient().getUser().getId();    // 보낸 사람이 developer면 client가 수신자

        unreadCountService.increment(roomId, receiverId);

        return convertToDto(savedMessage);
    }

    @Transactional
    public MessageResponseDto saveSystemMessage(Long roomId, String email, boolean isEntering) {

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new MessageException(ErrorCode.USER_NOT_FOUND)
        );

        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new MessageException(ErrorCode.CHATROOM_NOT_FOUND)
        );

        // 참여자 검증
        boolean isParticipant = chatRoom.getClient().getUser().getId().equals(user.getId())
                || chatRoom.getDeveloper().getUser().getId().equals(user.getId());

        if (!isParticipant) {
            throw new MessageException(ErrorCode.USER_FORBIDDEN);
        }

        // 입장 시에만 unread count 초기화
        if (isEntering) {
            unreadCountService.reset(roomId, user.getId());
        }

        // 시스템 메시지 내용 생성
        String content = isEntering
                ? user.getName() + "님이 입장했습니다."
                : user.getName() + "님이 퇴장했습니다.";

        Message message = Message.builder()
                .chatRoom(chatRoom)
                .user(user)
                .content(content)
                .isSystem(true)
                .build();

        Message savedMessage = messageRepository.save(message);

        return convertToDto(savedMessage);
    }

    @Transactional(readOnly = true)
    public MessageCursorResponseDto getMessages(Long roomId, Long lastMessageId, Pageable pageable, String email) {

        // user 가져오기
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new MessageException(ErrorCode.USER_NOT_FOUND)
        );

        // chatRoom 가져오기
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new MessageException(ErrorCode.CHATROOM_NOT_FOUND)
        );

        // 해당 채팅방 참여자인지 검증
        boolean isParticipant = chatRoom.getClient().getUser().getId().equals(user.getId())
                || chatRoom.getDeveloper().getUser().getId().equals(user.getId());

        if (!isParticipant) {
            throw new MessageException(ErrorCode.USER_FORBIDDEN);
        }

        // pageSize + 1개 조회 (hasNext 판단용)
        List<Message> messages = messageRepository.findMessages(roomId, lastMessageId, pageable);

        // hasNext 판단
        boolean hasNext = messages.size() > pageable.getPageSize();

        // 실제 반환할 데이터 (pageSize + 1에서 마지막 제거)
        if (hasNext) {
            messages = messages.subList(0, pageable.getPageSize());
        }

        // nextCursor 설정 (마지막 메시지의 id)
        Long nextCursor = hasNext ? messages.get(messages.size() - 1).getId() : null;

        List<MessageResponseDto> messageResponseDtos = messages.stream()
                .map(this::convertToDto)
                .toList();

        return MessageCursorResponseDto.builder()
                .messages(messageResponseDtos)
                .hasNext(hasNext)
                .nextCursor(nextCursor)
                .build();
    }

    private MessageResponseDto convertToDto(Message message) {
        return MessageResponseDto.builder()
                .id(message.getId())
                .senderId(message.getUser().getId())
                .senderName(message.getUser().getName())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .isSystem(message.getIsSystem())
                .sentAt(message.getCreatedAt())
                .build();
    }
}
