package com.example.readys7project.domain.chat.message.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record MessageCursorResponseDto(
        List<MessageResponseDto> messages,
        boolean hasNext,
        Long nextCursor
) {}
