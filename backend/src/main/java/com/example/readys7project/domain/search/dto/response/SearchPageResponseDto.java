package com.example.readys7project.domain.search.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record SearchPageResponseDto<T>(

        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages

) {
    public static <T> SearchPageResponseDto<T> from(Page<T> page) {
        return new SearchPageResponseDto<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}

