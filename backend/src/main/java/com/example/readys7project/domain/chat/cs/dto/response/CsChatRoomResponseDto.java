package com.example.readys7project.domain.chat.cs.dto.response;

import com.example.readys7project.domain.chat.cs.enums.CsStatus;
import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record CsChatRoomResponseDto(
    Long id,
    String title,
    Long inquirerId,
    String inquirerName,
    CsStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
