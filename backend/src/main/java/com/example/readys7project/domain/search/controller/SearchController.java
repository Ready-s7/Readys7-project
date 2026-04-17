package com.example.readys7project.domain.search.controller;

import com.example.readys7project.domain.search.dto.response.PopularRankingResponseDto;
import com.example.readys7project.domain.search.dto.response.TotalSearchResponseDto;
import com.example.readys7project.domain.search.service.SearchService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/v1/search/all")
    public ResponseEntity<ApiResponseDto<TotalSearchResponseDto>> getTotalSearchV1(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, searchService.getTotalSearchV1(userId, keyword, pageable)));
    }


    // Redis ZSet 적용 해야됨 (인기검색어 조회)
    @GetMapping("/v1/search/popular")
    public ResponseEntity<ApiResponseDto<List<PopularRankingResponseDto>>> getPopularRanking(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, searchService.getPopularRanking(limit)));
    }

    // Caffeine 사용
    @GetMapping("/v2/search/all")
    public ResponseEntity<ApiResponseDto<TotalSearchResponseDto>> getTotalSearchV2(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        Long userId = customUserDetails.getUser().getId();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, searchService.getTotalSearchV2(userId, keyword, pageable)));
    }

}
