package com.example.readys7project.domain.search.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

// 프로젝트 인기검색 DTO (캐시 사용 안 하는 버전)

@Builder
public record ProjectsGlobalSearchResponseDto(
        Long id,
        String title,
        String description,
        String category,
        Long minBudget,
        Long maxBudget,
        Integer duration,
        List<String> skills,
        String status,
        Integer currentProposalCount,
        Integer maxProposalCount,
        String clientName,
        Double clientRating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}
