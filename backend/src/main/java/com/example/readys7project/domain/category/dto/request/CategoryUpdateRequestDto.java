package com.example.readys7project.domain.category.dto.request;

import lombok.Builder;

@Builder
public record CategoryUpdateRequestDto(

        String name,

        String icon,

        String description,

        Integer displayOrder
) { }
