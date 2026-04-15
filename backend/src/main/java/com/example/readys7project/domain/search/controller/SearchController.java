package com.example.readys7project.domain.search.controller;

import com.example.readys7project.domain.search.dto.response.PopularSearchResponseDto;
import com.example.readys7project.domain.search.service.SearchService;
import com.example.readys7project.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/v1/popular")
    public ResponseEntity<ApiResponseDto<PopularSearchResponseDto>> getPopular(
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, searchService.getPopular(keyword, pageable)));
    }

}
