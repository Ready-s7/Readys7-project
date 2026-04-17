package com.example.readys7project.domain.search.dto.response;

import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record GlobalSearchResponseDto(

        Page<ProjectsGlobalSearchResponseDto> projects,

        Page<CategoriesGlobalSearchResponseDto> categories,

        Page<SkillsGlobalSearchResponseDto> skills,

        Page<DeveloperGlobalSearchResponseDto> developers

) {
    public static GlobalSearchResponseDto of(
            Page<ProjectsGlobalSearchResponseDto> projects,
            Page<CategoriesGlobalSearchResponseDto> categories,
            Page<SkillsGlobalSearchResponseDto> skills,
            Page<DeveloperGlobalSearchResponseDto> developers
    ) {
        return GlobalSearchResponseDto.builder()
                .projects(projects)
                .skills(skills)
                .categories(categories)
                .developers(developers)
                .build();
    }
}
