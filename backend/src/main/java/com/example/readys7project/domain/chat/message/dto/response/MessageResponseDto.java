package com.example.readys7project.domain.chat.message.dto.response;

import com.example.readys7project.domain.chat.message.enums.MessageEventType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MessageResponseDto(
        Long id,
        Long chatRoomId,
        Long senderId,
        String senderName,
        String content,
        MessageEventType eventType,
        Boolean isRead,
        Boolean isSystem,
        LocalDateTime sentAt
) {}
