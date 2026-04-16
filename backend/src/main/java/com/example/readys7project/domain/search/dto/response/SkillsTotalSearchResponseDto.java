package com.example.readys7project.domain.search.dto.response;

import com.example.readys7project.domain.skill.enums.SkillCategory;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SkillsTotalSearchResponseDto(
        Long id,
        Long adminId,
        String adminName,
        String name,
        SkillCategory category,
        LocalDateTime createdAt
) {}
