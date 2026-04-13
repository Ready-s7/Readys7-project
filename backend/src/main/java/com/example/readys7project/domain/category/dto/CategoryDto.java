package com.example.readys7project.domain.category.dto;

import lombok.Builder;

@Builder
public record CategoryDto(
        Long id,
        String name,
        String icon,
        String description,
        Integer displayOrder
) {}
