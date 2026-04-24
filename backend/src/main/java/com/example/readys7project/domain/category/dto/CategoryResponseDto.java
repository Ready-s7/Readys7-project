package com.example.readys7project.domain.category.dto;

import lombok.Builder;

@Builder
public record CategoryResponseDto(
        Long id,
        Long adminId,
        String name,
        String icon,
        String description,
        Integer displayOrder
) {}
