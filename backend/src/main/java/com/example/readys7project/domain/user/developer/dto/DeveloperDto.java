package com.example.readys7project.domain.user.developer.dto;

import com.example.readys7project.domain.user.enums.ParticipateType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record DeveloperDto(
        Long id,
        Long userId,
        String name,
        String title,
        Double rating,
        Integer reviewCount,
        Integer completedProjects,
        List<String> skills,          // JSON 문자열
        Integer minHourlyPay,
        Integer maxHourlyPay,
        String responseTime,
        String description,     // User에서 가져옴
        Boolean availableForWork,
        ParticipateType participateType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}