package com.example.readys7project.domain.search.dto.response;

import com.example.readys7project.domain.user.enums.ParticipateType;

import java.time.LocalDateTime;
import java.util.List;

public record DeveloperGlobalSearchResponseDto(
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
) {}
