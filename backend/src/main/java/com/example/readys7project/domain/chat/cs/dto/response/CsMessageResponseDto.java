package com.example.readys7project.domain.chat.cs.dto.response;

import com.example.readys7project.domain.chat.message.enums.MessageEventType;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record CsMessageResponseDto(
    Long id,
    Long senderId,
    String senderName,
    String content,
    MessageEventType eventType,
    Boolean isRead,
    LocalDateTime createdAt
) {}
