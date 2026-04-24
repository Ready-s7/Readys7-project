package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.search.dto.response.*;
import com.example.readys7project.domain.search.util.ValidateSearchKeyword;
import com.example.readys7project.global.exception.domain.SearchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @InjectMocks
    private SearchService searchService;

    @Mock
    private SearchQueryService searchQueryService;

    @Mock
    private SearchRankingService searchRankingService;

    @Mock
    private ValidateSearchKeyword validate;

    @Test
    @DisplayName("통합 검색 V1 - 검색 결과가 있는 경우 랭킹 업데이트 호출")
    void searchV1_WithResults_UpdatesRanking() {
        // given
        String keyword = "java";
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        given(validate.validateSearchKeyword(keyword)).willReturn(Optional.of(keyword));
        
        // Mocking SearchQueryService result
        GlobalSearchResponseDto mockResponse = createMockResponse(pageable, true);
        given(searchQueryService.fetchGlobalSearch(eq(keyword), any(Pageable.class))).willReturn(mockResponse);

        // when
        GlobalSearchResponseDto result = searchService.searchV1(userId, keyword, pageable);

        // then
        assertThat(result.projects().content()).isNotEmpty();
        verify(searchRankingService, times(1)).updateRankingCount(keyword, userId);
    }

    @Test
    @DisplayName("통합 검색 V1 - 검색 결과가 없는 경우 랭킹 업데이트 미호출")
    void searchV1_NoResults_DoesNotUpdateRanking() {
        // given
        String keyword = "noresult";
        Pageable pageable = PageRequest.of(0, 10);
        
        given(validate.validateSearchKeyword(keyword)).willReturn(Optional.of(keyword));
        
        GlobalSearchResponseDto mockResponse = createMockResponse(pageable, false);
        given(searchQueryService.fetchGlobalSearch(eq(keyword), any(Pageable.class))).willReturn(mockResponse);

        // when
        GlobalSearchResponseDto result = searchService.searchV1(null, keyword, pageable);

        // then
        assertThat(result.projects().content()).isEmpty();
        verify(searchRankingService, never()).updateRankingCount(anyString(), any());
    }

    @Test
    @DisplayName("통합 검색 V1 - 유효하지 않은 키워드 입력 시 빈 결과 반환")
    void searchV1_InvalidKeyword_ReturnsEmpty() {
        // given
        String keyword = "j";
        Pageable pageable = PageRequest.of(0, 10);
        given(validate.validateSearchKeyword(keyword)).willReturn(Optional.empty());

        // when
        GlobalSearchResponseDto result = searchService.searchV1(null, keyword, pageable);

        // then
        assertThat(result.projects().content()).isEmpty();
        verify(searchQueryService, never()).fetchGlobalSearch(anyString(), any());
    }

    @Test
    @DisplayName("통합 검색 V1 - 키워드 길이 초과 시 SearchException 전파")
    void searchV1_KeywordTooLong_ThrowsSearchException() {
        // given
        String longKeyword = "thiskeywordiswaytoolongtobevalid";
        Pageable pageable = PageRequest.of(0, 10);
        given(validate.validateSearchKeyword(longKeyword))
                .willThrow(new SearchException(com.example.readys7project.global.exception.common.ErrorCode.SEARCH_LENGTH_TOO_LONG));

        // when & then
        assertThatThrownBy(() -> searchService.searchV1(null, longKeyword, pageable))
                .isInstanceOf(SearchException.class);
    }

    @Test
    @DisplayName("인기 검색어 조회 위임 확인")
    void getPopularRanking_DelegatesToRankingService() {
        // given
        int limit = 5;
        given(searchRankingService.getPopularRanking(limit)).willReturn(Collections.emptyList());

        // when
        searchService.getPopularRanking(limit);

        // then
        verify(searchRankingService, times(1)).getPopularRanking(limit);
    }

    // 테스트용 Mock 응답 생성 헬퍼 메서드
    private GlobalSearchResponseDto createMockResponse(Pageable pageable, boolean hasResults) {
        Page<ProjectsGlobalSearchResponseDto> projectPage = hasResults 
                ? new PageImpl<>(List.of(mock(ProjectsGlobalSearchResponseDto.class))) 
                : Page.empty(pageable);
        
        return new GlobalSearchResponseDto(
                SearchPageResponseDto.from(projectPage),
                SearchPageResponseDto.from(Page.empty(pageable)),
                SearchPageResponseDto.from(Page.empty(pageable)),
                SearchPageResponseDto.from(Page.empty(pageable))
        );
    }
}
