package com.example.readys7project.domain.search.dto.response;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public record GlobalSearchResponseDto(

        SearchPageResponseDto<ProjectsGlobalSearchResponseDto> projects,

        SearchPageResponseDto<CategoriesGlobalSearchResponseDto> categories,

        SearchPageResponseDto<SkillsGlobalSearchResponseDto> skills,

        SearchPageResponseDto<DeveloperGlobalSearchResponseDto> developers

) {
    public static GlobalSearchResponseDto of(
            Page<ProjectsGlobalSearchResponseDto> projects,
            Page<CategoriesGlobalSearchResponseDto> categories,
            Page<SkillsGlobalSearchResponseDto> skills,
            Page<DeveloperGlobalSearchResponseDto> developers
    ) {
        return GlobalSearchResponseDto.builder()
                .projects(SearchPageResponseDto.from(projects))
                .skills(SearchPageResponseDto.from(skills))
                .categories(SearchPageResponseDto.from(categories))
                .developers(SearchPageResponseDto.from(developers))
                .build();
    }
}
