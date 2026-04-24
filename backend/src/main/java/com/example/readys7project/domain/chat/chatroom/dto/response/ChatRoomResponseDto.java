package com.example.readys7project.domain.chat.chatroom.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatRoomResponseDto(

        Long id,
        Long projectId,
        String projectTitle,
        Long clientId,
        String clientName,
        Long developerId,
        String developerName,
        Long unreadCount,
        LocalDateTime createdAt
) {
}
