package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.search.dto.response.*;
import com.example.readys7project.domain.search.util.ValidateSearchKeyword;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
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
    private ProjectRepository projectRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private DeveloperRepository developerRepository;

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
        
        given(validate.validateSearchKeyword(keyword)).willReturn(keyword);
        
        // Mocking results
        Page<ProjectsGlobalSearchResponseDto> projectPage = new PageImpl<>(List.of(mock(ProjectsGlobalSearchResponseDto.class)));
        given(projectRepository.projectsGlobalSearch(anyString(), any(Pageable.class)))
                .willReturn(SearchPageResponseDto.from(projectPage));
        
        given(categoryRepository.categoriesGlobalSearch(anyString(), any(Pageable.class)))
                .willReturn(SearchPageResponseDto.from(Page.empty(pageable)));
        given(skillRepository.skillsGlobalSearch(anyString(), any(Pageable.class)))
                .willReturn(SearchPageResponseDto.from(Page.empty(pageable)));
        given(developerRepository.developerGlobalSearch(anyString(), any(Pageable.class)))
                .willReturn(SearchPageResponseDto.from(Page.empty(pageable)));

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
        
        given(validate.validateSearchKeyword(keyword)).willReturn(keyword);
        
        given(projectRepository.projectsGlobalSearch(anyString(), any(Pageable.class)))
                .willReturn(SearchPageResponseDto.from(Page.empty(pageable)));
        given(categoryRepository.categoriesGlobalSearch(anyString(), any(Pageable.class)))
                .willReturn(SearchPageResponseDto.from(Page.empty(pageable)));
        given(skillRepository.skillsGlobalSearch(anyString(), any(Pageable.class)))
                .willReturn(SearchPageResponseDto.from(Page.empty(pageable)));
        given(developerRepository.developerGlobalSearch(anyString(), any(Pageable.class)))
                .willReturn(SearchPageResponseDto.from(Page.empty(pageable)));

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
        given(validate.validateSearchKeyword(keyword)).willReturn(null);

        // when
        GlobalSearchResponseDto result = searchService.searchV1(null, keyword, pageable);

        // then
        assertThat(result.projects().content()).isEmpty();
        verify(projectRepository, never()).projectsGlobalSearch(anyString(), any());
    }

    @Test
    @DisplayName("통합 검색 V1 - DB 에러 발생 시 SearchException 발생")
    void searchV1_DbError_ThrowsSearchException() {
        // given
        String keyword = "java";
        Pageable pageable = PageRequest.of(0, 10);
        given(validate.validateSearchKeyword(keyword)).willReturn(keyword);
        given(projectRepository.projectsGlobalSearch(anyString(), any(Pageable.class)))
                .willThrow(new RuntimeException("DB Error"));

        // when & then
        assertThatThrownBy(() -> searchService.searchV1(null, keyword, pageable))
                .isInstanceOf(SearchException.class);
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
    @DisplayName("통합 검색 V1 - 유효하지 않은 문자 포함 시 SearchException 전파")
    void searchV1_InvalidCharacter_ThrowsSearchException() {
        // given
        String invalidKeyword = "java' OR '1'='1";
        Pageable pageable = PageRequest.of(0, 10);
        given(validate.validateSearchKeyword(invalidKeyword))
                .willThrow(new SearchException(com.example.readys7project.global.exception.common.ErrorCode.SEARCH_INVALID_CHARACTER));

        // when & then
        assertThatThrownBy(() -> searchService.searchV1(null, invalidKeyword, pageable))
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
}
