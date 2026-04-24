package com.example.readys7project.domain.project.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.entity.QCategory;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.search.dto.response.ProjectsGlobalSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.SearchPageResponseDto;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProjectQueryRepositoryImpl implements ProjectQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    QProject qProject = QProject.project;


    /**
     * fulltext 적용
     * project 하이브리드 통합검색 적용.
     */

    // QueryDSL/JPQL은 MATCH ... AGAINST 같은 MySQL FULLTEXT 문법을 자연스럽게 처리하기 어렵다.
    // 그래서 FULLTEXT는 native SQL로 직접 실행하려고 EntityManager를 사용한다.
    // MATCH ... AGAINST = 텍스트 검색
    // native SQL은 JPA나 QueryDSL 문법이 아니라, DB에 직접 보내는 “진짜 SQL 문장”을 뜻한다.
    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public Page<Project> searchProjects(
            String keyword,
            Category category,
            ProjectStatus status,
            List<String> skills,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        if (keyword != null && !keyword.isBlank()) {
            builder.and(qProject.title.containsIgnoreCase(keyword)
                    .or(qProject.description.containsIgnoreCase(keyword)));
        }

        if (category != null) {
            builder.and(qProject.category.eq(category));
        }

        if (status != null) {
            builder.and(qProject.status.eq(status));
        }

        if (skills != null && !skills.isEmpty()) {
            BooleanBuilder skillBuilder = new BooleanBuilder();

            for (String skill : skills) {
                skillBuilder.or(
                        Expressions.booleanTemplate(
                                "JSON_CONTAINS({0}, JSON_QUOTE({1}))",
                                qProject.skills,
                                skill
                        )
                );
            }

            builder.and(skillBuilder);
        }

        // 조건에 맞는 실제 데이터를 조회
        List<Project> content = jpaQueryFactory
                .selectFrom(qProject)
                .leftJoin(qProject.category).fetchJoin()
                .leftJoin(qProject.client).fetchJoin()
                .leftJoin(qProject.client.user).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qProject.createdAt.desc())
                .fetch();

        //  전체 개수 조회
        Long total = jpaQueryFactory
                .select(qProject.count())
                .from(qProject)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    // 특정 클라이언트가 등록한 프로젝트 목록 조회
    @Override
    public Page<Project> findByClientWithPageable(Long clientId, Pageable pageable) {
        QClient qClient = QClient.client;
        QUser qUser = QUser.user;

        List<Project> content = jpaQueryFactory
                .selectFrom(qProject)
                .join(qProject.client, qClient).fetchJoin()
                .join(qClient.user, qUser).fetchJoin()
                .where(qProject.client.id.eq(clientId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qProject.createdAt.desc())
                .fetch();

        Long total = jpaQueryFactory
                .select(qProject.count())
                .from(qProject)
                .where(qProject.client.id.eq(clientId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    // 통합검색에서 프로젝트 검색 시 N-gram Full-text 인덱스를 사용하여 초고속 검색 수행
    @Override
    public SearchPageResponseDto<ProjectsGlobalSearchResponseDto> projectsGlobalSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();
        return projectsGlobalSearchByFullText(trimmedKeyword, pageable);
    }

    /**
     * N-gram Full-text 기반 프로젝트 검색
     */
    private SearchPageResponseDto<ProjectsGlobalSearchResponseDto> projectsGlobalSearchByFullText(String keyword, Pageable pageable) {

        QCategory qCategory = QCategory.category;
        QClient qClient = QClient.client;
        QUser qUser = QUser.user;

        List<Long> projectIds = findProjectIdsByFullText(keyword, pageable);

        if (projectIds.isEmpty()) {
            return SearchPageResponseDto.from(Page.empty(pageable));
        }

        List<ProjectsGlobalSearchResponseDto> content = jpaQueryFactory
                .select(Projections.constructor(ProjectsGlobalSearchResponseDto.class,
                        qProject.id,
                        qProject.title,
                        qProject.description,
                        qCategory.name,
                        qProject.minBudget,
                        qProject.maxBudget,
                        qProject.duration,
                        qProject.skills,
                        qProject.status.stringValue(),
                        qProject.currentProposalCount,
                        qProject.maxProposalCount,
                        qUser.name,
                        qClient.rating,
                        qProject.createdAt,
                        qProject.updatedAt
                ))
                .from(qProject)
                .leftJoin(qProject.category, qCategory)
                .leftJoin(qProject.client, qClient)
                .leftJoin(qClient.user, qUser)
                .where(qProject.id.in(projectIds))
                .orderBy(qProject.createdAt.desc())
                .fetch();

        long total = countProjectsByFullText(keyword);
        Page<ProjectsGlobalSearchResponseDto> page = new PageImpl<>(content, pageable, total);

        return SearchPageResponseDto.from(page);
    }

    // MATCH ... AGAINST 같은 MySQL FULLTEXT 문법은
    // QueryDSL/JPQL에서 깔끔하게 처리하기 어렵기 때문에, native SQL로 분리
    // FULLTEXT 검색으로 프로젝트 ID 목록을 찾는 함수 (title, description은 전문검색, skills는 정확한 매칭)
    private List<Long> findProjectIdsByFullText(String keyword, Pageable pageable) {
        String sql = """
                SELECT p.id
                FROM projects p
                WHERE p.is_deleted = false
                  AND (
                    MATCH(p.title, p.description) AGAINST (:keyword IN BOOLEAN MODE)
                    OR JSON_CONTAINS(p.skills, JSON_QUOTE(:keyword))
                  )
                ORDER BY p.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        @SuppressWarnings("unchecked")
        List<Number> result = entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword) // + 제거
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", pageable.getOffset())
                .getResultList();

        return result.stream()
                .map(Number::longValue)
                .toList();
    }

    // 이 메서드는 FULLTEXT 검색 결과가 총 몇 개인지 세는 함수
    private long countProjectsByFullText(String keyword) {
        String sql = """
                SELECT COUNT(*)
                FROM projects p
                WHERE p.is_deleted = false
                  AND (
                    MATCH(p.title, p.description) AGAINST (:keyword IN BOOLEAN MODE)
                    OR JSON_CONTAINS(p.skills, JSON_QUOTE(:keyword))
                  )
                """;

        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword) // + 제거
                .getSingleResult();

        return count.longValue();
    }

}


