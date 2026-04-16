package com.example.readys7project.domain.search.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record TotalSearchResponseDto(

        Page<ProjectsTotalSearchResponseDto> projects,

        Page<CategoriesTotalSearchResponseDto> categories,

        Page<SkillsTotalSearchResponseDto> skills

) {
    public static TotalSearchResponseDto of(
            Page<ProjectsTotalSearchResponseDto> projects,
            Page<CategoriesTotalSearchResponseDto> categories,
            Page<SkillsTotalSearchResponseDto> skills
    ) {
        return TotalSearchResponseDto.builder()
                .projects(projects)
                .skills(skills)
                .categories(categories)
                .build();
    }
}
