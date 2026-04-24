package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.search.dto.response.GlobalSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.PopularRankingResponseDto;
import com.example.readys7project.domain.search.util.ValidateSearchKeyword;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SearchQueryService searchQueryService;
    private final SearchRankingService searchRankingService;
    private final ValidateSearchKeyword validate;


    // V1 캐시 사용 X
    @Transactional
    public GlobalSearchResponseDto searchV1(Long userId, String keyword, Pageable pageable) {
        return validate.validateSearchKeyword(keyword)
                .map(trimKeyword -> {
                    GlobalSearchResponseDto result = searchQueryService.fetchGlobalSearch(trimKeyword, pageable);
                    if (hasAnyResult(result)) {
                        searchRankingService.updateRankingCount(trimKeyword, userId);
                    }
                    return result;
                })
                .orElseGet(() -> empty(pageable));
    }

    // V2 레디스 적용
    @Transactional
    public GlobalSearchResponseDto searchV2(Long userId, String keyword, Pageable pageable) {
        return validate.validateSearchKeyword(keyword)
                .map(trimKeyword -> {
                    GlobalSearchResponseDto result = searchQueryService.fetchGlobalSearch(trimKeyword, pageable);
                    if (hasAnyResult(result)) {
                        searchRankingService.updateRankingCount(trimKeyword, userId);
                    }
                    return result;
                })
                .orElseGet(() -> empty(pageable));
    }

    // 인기 검색어 조회
    public List<PopularRankingResponseDto> getPopularRanking(int limit) {
        // RankingService에 위임
        return searchRankingService.getPopularRanking(limit);
    }

    // 검색 결과 존재 여부 확인하는 로직
    private boolean hasAnyResult(GlobalSearchResponseDto result) {
        return !result.projects().content().isEmpty() ||
                !result.categories().content().isEmpty() ||
                !result.skills().content().isEmpty() ||
                !result.developers().content().isEmpty();
    }

    // 빈 값을 리턴해주는 공용 정적 팩토리 메서드
    public static GlobalSearchResponseDto empty(Pageable pageable) {
        return GlobalSearchResponseDto.of(
                Page.empty(pageable),
                Page.empty(pageable),
                Page.empty(pageable),
                Page.empty(pageable)
        );
    }
}
