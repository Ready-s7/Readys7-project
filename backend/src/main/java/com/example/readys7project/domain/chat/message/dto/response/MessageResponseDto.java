package com.example.readys7project.domain.chat.message.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MessageResponseDto(
        Long id,
        Long senderId,
        String senderName,
        String content,
        Boolean isRead,
        Boolean isSystem,
        LocalDateTime sentAt
) {}
