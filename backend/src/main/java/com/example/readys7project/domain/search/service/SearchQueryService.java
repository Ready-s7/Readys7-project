package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.search.dto.response.*;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchQueryService {

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final DeveloperRepository developerRepository;
    private final Executor taskExecutor;

    @Transactional(readOnly = true)
    @Cacheable(value = "globalSearch",
            key = "T(com.example.readys7project.domain.search.util.SearchCacheKeyGenerator).generate(#keyword, #pageable)")
    public GlobalSearchResponseDto fetchGlobalSearch(String keyword, Pageable pageable) {
        try {
            // 4개의 쿼리를 비동기 병렬로 실행
            CompletableFuture<SearchPageResponseDto<ProjectsGlobalSearchResponseDto>> projectFuture =
                    CompletableFuture.supplyAsync(() -> projectRepository.projectsGlobalSearch(keyword, pageable), taskExecutor);

            CompletableFuture<SearchPageResponseDto<CategoriesGlobalSearchResponseDto>> categoryFuture =
                    CompletableFuture.supplyAsync(() -> categoryRepository.categoriesGlobalSearch(keyword, pageable), taskExecutor);

            CompletableFuture<SearchPageResponseDto<SkillsGlobalSearchResponseDto>> skillFuture =
                    CompletableFuture.supplyAsync(() -> skillRepository.skillsGlobalSearch(keyword, pageable), taskExecutor);

            CompletableFuture<SearchPageResponseDto<DeveloperGlobalSearchResponseDto>> developerFuture =
                    CompletableFuture.supplyAsync(() -> developerRepository.developerGlobalSearch(keyword, pageable), taskExecutor);

            // 모든 작업이 완료될 때까지 대기
            CompletableFuture.allOf(projectFuture, categoryFuture, skillFuture, developerFuture).join();

            return new GlobalSearchResponseDto(
                    projectFuture.get(),
                    categoryFuture.get(),
                    skillFuture.get(),
                    developerFuture.get()
            );

        } catch (Exception e) {
            log.warn("통합 검색 중 병렬 처리 에러 발생 - 키워드: [{}], 메시지: {}", keyword, e.getMessage());
            throw new SearchException(ErrorCode.SEARCH_FAILED);
        }
    }
}
