package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.search.Repository.SearchRankingRepository;
import com.example.readys7project.domain.search.dto.response.CategoriesTotalSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.ProjectsTotalSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.SkillsTotalSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.TotalSearchResponseDto;
import com.example.readys7project.domain.search.entity.SearchRanking;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SearchException;
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
public class SearchRankingService {

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final SearchRankingRepository searchRankingRepository;

    /* 어떤 사용자가 악의적으로 수만자의 긴 텍스트를 검색창에 넣으면 서버 메모리가 감당을 못할 수 있음,
       이러한 상황을 방지하기 위한 방어코드*/
    private static final int MAX_KEYWORD = 20;

    // 랭킹 업데이트를 위해서 readOnly 제거
    @Transactional
    public TotalSearchResponseDto getTotalSearchV1(String keyword, Pageable pageable) {

        // 검색값 (keyword)가 빈값인지 체크
        if (keyword == null || keyword.isBlank()) {
            // NPE 방지를 위해 빈 객체를 리턴
            return empty(pageable);
        }
        // 양 끝 공백 제거
        String trimKeyword = keyword.trim();

        // 의미 없는 검색 방지 (ex 1글자 검색)
        if (trimKeyword.length() < 2) {
            return empty(pageable);
        }

        // 최대 검색 길이를 초과한 경우 예외 던지기
        if (trimKeyword.length() > MAX_KEYWORD) {
            throw new SearchException(ErrorCode.SEARCH_LENGTH_TOO_LONG);
        }

        updateRankingCount(trimKeyword);

        try {
            Page<ProjectsTotalSearchResponseDto> projectPage = projectRepository.projectsTotalSearch(trimKeyword, pageable);
            Page<CategoriesTotalSearchResponseDto> categoryPage = categoryRepository.categoriesTotalSearch(trimKeyword, pageable);
            Page<SkillsTotalSearchResponseDto> skillPage = skillRepository.skillsTotalSearch(trimKeyword, pageable);

            return new TotalSearchResponseDto(projectPage, categoryPage, skillPage);
        } catch (Exception e) {
            log.warn("통합 검색 중 DB 에러 발생 - 키워드: [{}], 메시지: {}", trimKeyword, e.getMessage());
            throw new SearchException(ErrorCode.SEARCH_FAILED);
        }
    }

    private void updateRankingCount(String keyword) {
        SearchRanking searchRanking = searchRankingRepository.findByKeywordAndIsDeletedFalse(keyword)
                .orElseGet( () -> new SearchRanking(keyword));
        // orElseGet -> 없으면 뒤에 명령문을 실행하라는 의미

        if (searchRanking.getId() != null) {
            // 이미 검색했던 값이면 +1
            searchRanking.increaseSearchCount();
        } else {
            // 처음 생긴 검색한 값이면 DB에 저장
            searchRankingRepository.save(searchRanking);
        }
    }


    // 상위 10개 인기 검색어 순위 조회
    @Transactional(readOnly = true)
    public List<String> getPopularRanking() {
        return searchRankingRepository.findTop10ByIsDeletedFalseOrderBySearchCountDesc()
                .stream()
                .map(SearchRanking::getKeyword)
                .toList();
    }

    // 빈 값을 리턴해주는 공용 정적 팩토리 메서드
    public static TotalSearchResponseDto empty(Pageable pageable) {
        return new TotalSearchResponseDto(
                Page.empty(pageable),
                Page.empty(pageable),
                Page.empty(pageable)
        );
    }

}
