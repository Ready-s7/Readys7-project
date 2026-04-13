package com.example.readys7project.domain.user.client.dto.response;

import com.example.readys7project.domain.user.enums.ParticipateType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UpdateClientProfileResponseDto(

        Long clientId,

        String title,

        ParticipateType participateType,

        LocalDateTime updatedAt
) {}
