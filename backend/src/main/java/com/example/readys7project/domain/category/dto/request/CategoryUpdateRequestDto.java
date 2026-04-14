package com.example.readys7project.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CategoryUpdateRequestDto(

        String name,

        String icon,

        String description,

        Integer displayOrder
) { }
