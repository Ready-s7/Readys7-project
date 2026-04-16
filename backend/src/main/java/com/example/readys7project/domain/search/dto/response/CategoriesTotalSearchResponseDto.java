package com.example.readys7project.domain.search.dto.response;

import lombok.Builder;

@Builder
public record CategoriesTotalSearchResponseDto(

        Long Id,

        String name,

        String icon
) {}
