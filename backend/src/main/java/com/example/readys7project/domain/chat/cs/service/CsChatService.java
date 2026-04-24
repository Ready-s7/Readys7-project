package com.example.readys7project.domain.chat.cs.service;

import com.example.readys7project.domain.chat.cs.dto.request.CsChatRoomRequestDto;
import com.example.readys7project.domain.chat.cs.dto.response.CsChatRoomResponseDto;
import com.example.readys7project.domain.chat.cs.dto.response.CsMessageResponseDto;
import com.example.readys7project.domain.chat.cs.entity.CsChatRoom;
import com.example.readys7project.domain.chat.cs.entity.CsMessage;
import com.example.readys7project.domain.chat.cs.enums.CsStatus;
import com.example.readys7project.domain.chat.cs.repository.CsChatRoomRepository;
import com.example.readys7project.domain.chat.cs.repository.CsMessageRepository;
import com.example.readys7project.domain.chat.message.enums.MessageEventType;
import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ChatRoomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CsChatService {

    private final CsChatRoomRepository csChatRoomRepository;
    private final CsMessageRepository csMessageRepository;
    private final UserRepository userRepository;

    // CS 채팅방 생성
    @Transactional
    public CsChatRoomResponseDto createCsRoom(CsChatRoomRequestDto request, String email) {
        User user = findUserByEmail(email);
        
        CsChatRoom room = CsChatRoom.builder()
                .inquirer(user)
                .title(request.title())
                .build();
        
        CsChatRoom savedRoom = csChatRoomRepository.save(room);
        return convertToDto(savedRoom);
    }

    // CS 채팅방 상세 조회
    @Transactional(readOnly = true)
    public CsChatRoomResponseDto getCsRoom(Long roomId, String email) {
        CsChatRoom room = findCsChatRoom(roomId);
        
        User user = findUserByEmail(email);
        if (!room.getInquirer().getId().equals(user.getId()) && user.getUserRole() != UserRole.ADMIN) {
            throw new ChatRoomException(ErrorCode.USER_FORBIDDEN);
        }
        
        return convertToDto(room);
    }

    // CS 채팅방 메시지 목록 조회
    @Transactional(readOnly = true)
    public List<CsMessageResponseDto> getCsMessages(Long roomId, String email) {
        CsChatRoom room = findCsChatRoom(roomId);
        
        User user = findUserByEmail(email);
        if (!room.getInquirer().getId().equals(user.getId()) && user.getUserRole() != UserRole.ADMIN) {
            throw new ChatRoomException(ErrorCode.USER_FORBIDDEN);
        }

        return csMessageRepository.findByCsChatRoomOrderByCreatedAtAsc(room)
                .stream()
                .map(this::convertToMessageDto)
                .collect(Collectors.toList());
    }

    // CS 메시지 저장
    @Transactional
    public CsMessageResponseDto saveMessage(Long roomId, SendMessageRequestDto request, String email) {
        CsChatRoom room = findCsChatRoom(roomId);
        
        User sender = findUserByEmail(email);
        
        CsMessage message = CsMessage.builder()
                .csChatRoom(room)
                .sender(sender)
                .content(request.content())
                .eventType(MessageEventType.SEND)
                .build();
        
        CsMessage savedMessage = csMessageRepository.save(message);
        return convertToMessageDto(savedMessage);
    }

    // CS 시스템 메시지 저장 (입장/퇴장)
    @Transactional
    public CsMessageResponseDto saveSystemMessage(Long roomId, String email, boolean isEnter) {
        CsChatRoom room = findCsChatRoom(roomId);
        
        User sender = findUserByEmail(email);
        String content = String.format("%s님이 %s하셨습니다.", sender.getName(), isEnter ? "입장" : "퇴장");
        
        CsMessage message = CsMessage.builder()
                .csChatRoom(room)
                .sender(sender)
                .content(content)
                .eventType(isEnter ? MessageEventType.ENTER : MessageEventType.LEAVE)
                .build();
        
        CsMessage savedMessage = csMessageRepository.save(message);
        return convertToMessageDto(savedMessage);
    }

    // 내 문의 목록 조회
    @Transactional(readOnly = true)
    public Page<CsChatRoomResponseDto> getMyCsRooms(int page, int size, String email) {
        User user = findUserByEmail(email);
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return csChatRoomRepository.findByInquirer(user, pageable).map(this::convertToDto);
    }

    // [관리자] 전체 문의 목록 조회 (상태 필터링)
    @Transactional(readOnly = true)
    public Page<CsChatRoomResponseDto> getAllCsRooms(CsStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        if (status != null) {
            return csChatRoomRepository.findByStatus(status, pageable).map(this::convertToDto);
        }
        return csChatRoomRepository.findAll(pageable).map(this::convertToDto);
    }

    // [관리자] 문의 상태 변경
    @Transactional
    public CsChatRoomResponseDto updateCsStatus(Long roomId, CsStatus newStatus) {
        CsChatRoom room = findCsChatRoom(roomId);
        
        room.updateStatus(newStatus);
        return convertToDto(room);
    }

    // CS 메시지 수정
    @Transactional
    public CsMessageResponseDto updateMessage(Long messageId, SendMessageRequestDto request, String email) {
        CsMessage message = csMessageRepository.findById(messageId)
                .orElseThrow(() -> new ChatRoomException(ErrorCode.MESSAGE_NOT_FOUND));
        
        User sender = findUserByEmail(email);
        if (!message.getSender().getId().equals(sender.getId())) {
            throw new ChatRoomException(ErrorCode.USER_FORBIDDEN);
        }

        message.updateContent(request.content());
        return convertToMessageDto(message);
    }

    // CS 메시지 삭제
    @Transactional
    public void deleteMessage(Long messageId, String email) {
        CsMessage message = csMessageRepository.findById(messageId)
                .orElseThrow(() -> new ChatRoomException(ErrorCode.MESSAGE_NOT_FOUND));
        
        User sender = findUserByEmail(email);
        if (!message.getSender().getId().equals(sender.getId())) {
            throw new ChatRoomException(ErrorCode.USER_FORBIDDEN);
        }

        csMessageRepository.delete(message);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ChatRoomException(ErrorCode.USER_NOT_FOUND));
    }

    private CsChatRoom findCsChatRoom(Long roomId) {
        return csChatRoomRepository.findById(roomId).orElseThrow(
                () -> new ChatRoomException(ErrorCode.CHATROOM_NOT_FOUND)
        );
    }

    private CsChatRoomResponseDto convertToDto(CsChatRoom room) {
        return CsChatRoomResponseDto.builder()
                .id(room.getId())
                .title(room.getTitle())
                .inquirerId(room.getInquirer().getId())
                .inquirerName(room.getInquirer().getName())
                .status(room.getStatus())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private CsMessageResponseDto convertToMessageDto(CsMessage msg) {
        return CsMessageResponseDto.builder()
                .id(msg.getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getName())
                .content(msg.getContent())
                .eventType(msg.getEventType())
                .isRead(msg.getIsRead())
                .createdAt(msg.getCreatedAt())
                .build();
    }
}
