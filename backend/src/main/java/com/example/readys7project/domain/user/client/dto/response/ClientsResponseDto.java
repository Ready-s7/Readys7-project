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
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.0")
        Double rating,
        Integer reviewCount,
        ParticipateType participateType,
        String description
) {}
