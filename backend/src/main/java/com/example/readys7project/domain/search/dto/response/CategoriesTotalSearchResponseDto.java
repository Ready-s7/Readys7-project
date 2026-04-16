package com.example.readys7project.domain.search.dto.response;

import lombok.Builder;

@Builder
public record CategoriesTotalSearchResponseDto(
        Long id,
        Long adminId,
        String name,
        String icon,
        String description,
        Integer displayOrder
) {}
