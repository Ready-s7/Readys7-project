package com.example.readys7project.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CategoryRequestDto(

        @NotBlank(message = "카테고리는 필수입니다.")
        String name,

        String icon,

        String description,

        @NotNull(message = "정렬 순서는 필수입니다.")
        Integer displayOrder
) { }
