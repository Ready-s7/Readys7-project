package com.example.readys7project.domain.user.developer.dto;

import com.example.readys7project.domain.user.developer.enums.ParticipateType;
import lombok.Builder;

@Builder
public record DeveloperDto(
        Long id,
        String name,
        String title,
        Double rating,
        Integer reviewCount,
        Integer completedProjects,
        String skills,          // JSON 문자열
        Integer minHourlyPay,
        Integer maxHourlyPay,
        String responseTime,
        String description,     // User에서 가져옴
        Boolean availableForWork,
        ParticipateType participateType
) {
}