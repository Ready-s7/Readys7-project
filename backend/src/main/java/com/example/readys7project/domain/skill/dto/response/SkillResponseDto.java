package com.example.readys7project.domain.skill.dto.response;

import com.example.readys7project.domain.skill.enums.SkillCategory;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SkillResponseDto(
        Long id,
        Long adminId,
        String adminName,
        String name,
        SkillCategory category,
        LocalDateTime createdAt
) {
}
