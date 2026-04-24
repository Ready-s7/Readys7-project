package com.example.readys7project.domain.user.developer.repository;

import com.example.readys7project.domain.category.entity.QCategory;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.proposal.entity.QProposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.search.dto.response.DeveloperGlobalSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.SearchPageResponseDto;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.entity.QDeveloper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DeveloperQueryRepositoryImpl implements DeveloperQueryRepository {

    private final JPAQueryFactory queryFactory;
    QDeveloper qDeveloper = QDeveloper.developer;


    /**
     * fulltext 적용
     * 개발자 통합검색을 하이브리드 방식으로 처리하는 코드
     */

    // JPA에서 native SQL을 직접 실행할 때 사용하는 객체.
    // MATCH ... AGAINST는 MySQL FULLTEXT 문법.
    @PersistenceContext
    private EntityManager entityManager;


    // 전체 개발자 목록을 사용자 정보와 함께 조회하는 메서드
    @Override
    public Page<Developer> findAllWithUser(Pageable pageable) {
        QUser qUser = QUser.user;

        List<Developer> content = queryFactory
                .selectFrom(qDeveloper)
                .join(qDeveloper.user, qUser).fetchJoin() // fetchJoin 추가
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qDeveloper.count())
                .from(qDeveloper)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<Developer> searchDevelopers(List<String> skills, Double minRating, Pageable pageable) {
        QUser qUser = QUser.user;
        BooleanBuilder builder = new BooleanBuilder();

        if (skills != null && !skills.isEmpty()) {
            BooleanBuilder skillBuilder = new BooleanBuilder();
            for (String s : skills) {
                if (s != null && !s.isBlank()) {
                    skillBuilder.or(
                            Expressions.booleanTemplate(
                                    "function('json_contains', UPPER({0}), function('json_quote', {1})) = 1",
                                    qDeveloper.skills,
                                    s.toUpperCase()
                            )
                    );
                }
            }
            builder.and(skillBuilder);
        }

        if (minRating != null) {
            builder.and(qDeveloper.rating.goe(minRating));
        }

        List<Developer> content = queryFactory
                .selectFrom(qDeveloper)
                .join(qDeveloper.user, qUser).fetchJoin() // fetchJoin 추가
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qDeveloper.count())
                .from(qDeveloper)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<Project> findMyProjects(Developer developer, Pageable pageable) {
        QProposal qProposal = QProposal.proposal;
        QProject qProject = QProject.project;
        QClient qClient = QClient.client;
        QUser qUser = QUser.user;
        QCategory qCategory = QCategory.category;

        List<Project> content = queryFactory
                .select(qProject)
                .from(qProposal)
                .join(qProposal.project, qProject)
                .join(qProject.client, qClient).fetchJoin()
                .join(qClient.user, qUser).fetchJoin()
                .join(qProject.category, qCategory).fetchJoin()
                .where(
                        qProposal.developer.eq(developer)
                                .and(qProposal.status.eq(ProposalStatus.ACCEPTED))
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qProposal.count())
                .from(qProposal)
                .where(
                        qProposal.developer.eq(developer)
                                .and(qProposal.status.eq(ProposalStatus.ACCEPTED))
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }


    // 통합검색에서 개발자 검색 시 N-gram Full-text 인덱스를 사용하여 초고속 검색 수행
    @Override
    public SearchPageResponseDto<DeveloperGlobalSearchResponseDto> developerGlobalSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();
        return developerGlobalSearchByFullText(trimmedKeyword, pageable);
    }

    // N-gram Full-text 기반 개발자 검색
    private SearchPageResponseDto<DeveloperGlobalSearchResponseDto> developerGlobalSearchByFullText(String keyword, Pageable pageable) {

        QUser qUser = QUser.user;

        List<Long> developerIds = findDeveloperIdsByFullText(keyword, pageable);

        if (developerIds.isEmpty()) {
            return SearchPageResponseDto.from(Page.empty(pageable));
        }

        // FULLTEXT로 찾은 id 목록을 기준으로 최종 응답 DTO를 조회
        // FIELD() 함수를 사용하여 검색된 ID 순서(최신순/정확도순)를 그대로 유지
        String fieldExpr = "FIELD({0}, " +
                developerIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")) + ")";

        List<DeveloperGlobalSearchResponseDto> content = queryFactory
                .select(Projections.constructor(DeveloperGlobalSearchResponseDto.class,
                        qDeveloper.id,
                        qUser.id,
                        qUser.name,
                        qDeveloper.title,
                        qDeveloper.rating,
                        qDeveloper.reviewCount,
                        qDeveloper.completedProjects,
                        qDeveloper.skills,
                        qDeveloper.minHourlyPay,
                        qDeveloper.maxHourlyPay,
                        qDeveloper.responseTime,
                        qUser.description,
                        qDeveloper.availableForWork,
                        qDeveloper.participateType,
                        qDeveloper.createdAt,
                        qDeveloper.updatedAt
                ))
                .from(qDeveloper)
                .leftJoin(qDeveloper.user, qUser)
                .where(qDeveloper.id.in(developerIds))
                .orderBy(Expressions.numberTemplate(Integer.class, fieldExpr, qDeveloper.id).asc())
                .fetch();

        long total = countDevelopersByFullText(keyword);
        Page<DeveloperGlobalSearchResponseDto> page = new PageImpl<>(content, pageable, total);

        return SearchPageResponseDto.from(page);
    }

    // FULLTEXT 검색으로 개발자 id 목록을 반환하는 메서드 (title, name, description은 전문검색, skills는 정확한 매칭)
    private List<Long> findDeveloperIdsByFullText(String keyword, Pageable pageable) {
        String sql = """
                SELECT d.id
                FROM developers d
                JOIN users u ON u.id = d.user_id
                WHERE d.is_deleted = false
                  AND u.is_deleted = false
                  AND (
                    MATCH(d.title) AGAINST (:keyword IN BOOLEAN MODE)
                    OR MATCH(u.name, u.description) AGAINST (:keyword IN BOOLEAN MODE)
                    OR JSON_CONTAINS(d.skills, JSON_QUOTE(:keyword))
                  )
                ORDER BY d.created_at DESC
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

    // FULLTEXT 검색 조건에 맞는 개발자 총 개수를 세는 메서드
    private long countDevelopersByFullText(String keyword) {
        String sql = """
                SELECT COUNT(*)
                FROM developers d
                JOIN users u ON u.id = d.user_id
                WHERE d.is_deleted = false
                  AND u.is_deleted = false
                  AND (
                    MATCH(d.title) AGAINST (:keyword IN BOOLEAN MODE)
                    OR MATCH(u.name, u.description) AGAINST (:keyword IN BOOLEAN MODE)
                    OR JSON_CONTAINS(d.skills, JSON_QUOTE(:keyword))
                  )
                """;

        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword) // + 제거
                .getSingleResult();

        return count.longValue();
    }

}



