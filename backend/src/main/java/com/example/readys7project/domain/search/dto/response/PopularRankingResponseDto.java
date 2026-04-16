package com.example.readys7project.domain.search.dto.response;

import lombok.Builder;

// 인기 순위 응답 DTO
@Builder
public record PopularRankingResponseDto (

        // 순위가 몇번째인지 명시적으로 보여주기 위해
        Integer ranking,

        String keyword

) {}

