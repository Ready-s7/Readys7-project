package com.example.readys7project.domain.search.dto.response;

import com.example.readys7project.domain.skill.enums.SkillCategory;
import lombok.Builder;

@Builder
public record SkillPopularSearchResponseDto (

        Long id,

        String name,

        SkillCategory skillCategory
) {}
