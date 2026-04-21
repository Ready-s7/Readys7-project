package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.search.dto.response.*;
import com.example.readys7project.domain.search.util.ValidateSearchKeyword;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final DeveloperRepository developerRepository;
    private final SearchRankingService searchRankingService;
    private final ValidateSearchKeyword validate;


    // V1 캐시 사용 X
    // 랭킹 업데이트를 위해서 readOnly 제거
    @Transactional
    public GlobalSearchResponseDto searchV1(Long userId, String keyword, Pageable pageable) {

        // 양 끝 공백 제거
        String trimKeyword = validate.validateSearchKeyword(keyword);

        // 한번 더 null 체크
        if (trimKeyword == null) {
            // NPE 방지를 위해 빈 객체를 리턴
            return empty(pageable);
        }

        // 미리 담아주고
        GlobalSearchResponseDto result = GlobalSearch(trimKeyword, pageable);

        // 검색 결과가 존재한다면, 업데이트 로직에게 넘기기
        if (hasAnyResult(result)) {
            searchRankingService.updateRankingCount(trimKeyword, userId);
        }

        return result;
    }

    // V2 레디스 적용
    @Transactional(readOnly = true)
    @Cacheable(value = "globalSearch",
            key = "T(com.example.readys7project.domain.search.util.SearchCacheKeyGenerator).generate(#keyword, #pageable)")

    public GlobalSearchResponseDto searchV2(String keyword, Pageable pageable) {

        String trimKeyword = validate.validateSearchKeyword(keyword);

        if (trimKeyword == null) {
            return empty(pageable);
        }

        return GlobalSearch(trimKeyword, pageable);
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

    // 공용 DTO 리턴 메서드
    private GlobalSearchResponseDto GlobalSearch(String keyword, Pageable pageable) {
        try {
            SearchPageResponseDto<ProjectsGlobalSearchResponseDto> projectPage =
                    projectRepository.projectsGlobalSearch(keyword, pageable);
            SearchPageResponseDto<CategoriesGlobalSearchResponseDto> categoryPage =
                    categoryRepository.categoriesGlobalSearch(keyword, pageable);
            SearchPageResponseDto<SkillsGlobalSearchResponseDto> skillPage =
                    skillRepository.skillsGlobalSearch(keyword, pageable);
            SearchPageResponseDto<DeveloperGlobalSearchResponseDto> developerPage =
                    developerRepository.developerGlobalSearch(keyword, pageable);

            return new GlobalSearchResponseDto(projectPage, categoryPage, skillPage, developerPage);

        } catch (Exception e) {
            log.warn("통합 검색 중 DB 에러 발생 - 키워드: [{}], 메시지: {}", keyword, e.getMessage());
            throw new SearchException(ErrorCode.SEARCH_FAILED);
        }
    }
}