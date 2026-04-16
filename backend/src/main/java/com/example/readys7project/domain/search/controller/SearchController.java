package com.example.readys7project.domain.search.controller;

import com.example.readys7project.domain.search.dto.response.TotalSearchResponseDto;
import com.example.readys7project.domain.search.service.SearchRankingService;
import com.example.readys7project.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchRankingService searchRankingService;

    @GetMapping("/v1/search/all")
    public ResponseEntity<ApiResponseDto<TotalSearchResponseDto>> getTotalSearchV1(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 5) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, searchRankingService.getTotalSearchV1(keyword, pageable)));
    }

    @GetMapping("/v1/search/popular")
    public ResponseEntity<ApiResponseDto<List<String>>> getPopularRanking() {
        List<String> ranking = searchRankingService.getPopularRanking();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, ranking));
    }

}
