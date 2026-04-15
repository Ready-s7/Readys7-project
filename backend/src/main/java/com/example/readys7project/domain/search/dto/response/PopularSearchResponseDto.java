package com.example.readys7project.domain.search.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record PopularSearchResponseDto(

        Page<ProjectPopularSearchResponseDto> projects,

        Page<CategoryPopularSearchResponseDto> categories,

        Page<SkillPopularSearchResponseDto> skills

) {}
