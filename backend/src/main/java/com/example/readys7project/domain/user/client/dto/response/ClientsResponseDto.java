package com.example.readys7project.domain.user.client.dto.response;

import com.example.readys7project.domain.user.enums.ParticipateType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

@Builder
public record ClientsResponseDto(
        Long id,
        Long userId,            // 추가
        String name,
        String title,
        Integer completedProject,
        Double rating,
        Integer reviewCount,
        ParticipateType participateType,
        String description
) {}
