package com.example.readys7project.domain.search.dto.response;

import lombok.Builder;

@Builder
public record CategoryPopularSearchResponseDto (

        Long Id,

        String name,

        String icon
) {}
