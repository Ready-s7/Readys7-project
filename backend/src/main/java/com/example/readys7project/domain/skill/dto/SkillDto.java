package com.example.readys7project.domain.skill.dto;

import com.example.readys7project.domain.skill.enums.SkillCategory;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SkillDto (
        Long id,
        Long adminId,
        String adminName,
        String name,
        SkillCategory category,
        LocalDateTime createdAt
) {
}
