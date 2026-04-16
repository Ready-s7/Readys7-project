package com.example.readys7project.domain.search.dto.response;

import com.example.readys7project.domain.project.enums.ProjectStatus;
import lombok.Builder;

// 프로젝트 인기검색 DTO (캐시 사용 안 하는 버전)

@Builder
public record ProjectsTotalSearchResponseDto(

        Long id,

        String title,

        Long minBudget,

        Long maxBudget,

        ProjectStatus status

) {}
