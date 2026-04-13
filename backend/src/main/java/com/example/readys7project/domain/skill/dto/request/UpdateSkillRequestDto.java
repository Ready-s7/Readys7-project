package com.example.readys7project.domain.skill.dto.request;

import com.example.readys7project.domain.skill.enums.SkillCategory;
import lombok.Builder;

@Builder
public record UpdateSkillRequestDto(
        String name,
        SkillCategory category
) {
}
