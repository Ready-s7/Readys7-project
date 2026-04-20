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

import java.util.List;

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
                .join(qDeveloper.user, qUser).fetchJoin()
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
                .join(qDeveloper.user, qUser).fetchJoin()
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
                .select(qProposal.project)
                .from(qProposal)
                .join(qProposal.project, qProject)
                .join(qProject.client, qClient)
                .join(qClient.user, qUser)
                .join(qProject.category, qCategory)
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


    // 하이브리드 검색 핵심 시작.
    @Override
    public SearchPageResponseDto<DeveloperGlobalSearchResponseDto> developerGlobalSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();

        // 검색어 안에 공백이 있으면 FULLTEXT로 간다.
        if (hasWhitespace(trimmedKeyword)) {
            return developerGlobalSearchByFullText(trimmedKeyword, pageable);
        }

        // 검색어 안에 공백이 없으면 LIKE기반으로 간다.
        return developerGlobalSearchByLike(trimmedKeyword, pageable);
    }


    // LIKE 기반으로 간다.
    private SearchPageResponseDto<DeveloperGlobalSearchResponseDto> developerGlobalSearchByLike(String keyword, Pageable pageable) {

        QUser qUser = QUser.user;

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
                // 즉 LIKE 경로에서는 이름만 검색
                .where(nameLike(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qDeveloper.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(qDeveloper.count())
                .from(qDeveloper)
                .leftJoin(qDeveloper.user, qUser)
                .where(nameLike(keyword));

        Page<DeveloperGlobalSearchResponseDto> page = PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
        return SearchPageResponseDto.from(page);
    }

    // 공백 포함 복합 검색어를 처리하는 FULLTEXT.
    private SearchPageResponseDto<DeveloperGlobalSearchResponseDto> developerGlobalSearchByFullText(String keyword, Pageable pageable) {

        QUser qUser = QUser.user;

        List<Long> developerIds = findDeveloperIdsByFullText(keyword, pageable);

        if (developerIds.isEmpty()) {
            return SearchPageResponseDto.from(Page.empty(pageable));
        }

        // FULLTEXT로 찾은 id 목록을 기준으로
        // 최종 응답 DTO를 조회
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
                .orderBy(qDeveloper.createdAt.desc())
                .fetch();

        long total = countDevelopersByFullText(keyword);
        Page<DeveloperGlobalSearchResponseDto> page = new PageImpl<>(content, pageable, total);

        return SearchPageResponseDto.from(page);
    }

    // FULLTEXT 검색으로 개발자 id 목록을 반환하는 메서드
    private List<Long> findDeveloperIdsByFullText(String keyword, Pageable pageable) {
        String sql = """
                SELECT d.id
                FROM developers d
                JOIN users u ON u.id = d.user_id
                WHERE d.is_deleted = false
                  AND u.is_deleted = false
                  AND (
                    MATCH(d.title) AGAINST (:keyword IN BOOLEAN MODE)
                    OR MATCH(u.description) AGAINST (:keyword IN BOOLEAN MODE)
                  )
                ORDER BY d.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        // WHERE d.is_deleted = false AND u.is_deleted = false -> 삭제되지 않은 데이터만 검색
        //  MATCH(d.title) AGAINST (:keyword IN BOOLEAN MODE) -> 개발자 title에 대해 FULLTEXT 검색
        // OR MATCH(u.description) AGAINST (:keyword IN BOOLEAN MODE) -> 사용자 description에 대해 FULLTEXT 검색

        // 컴파일러 경고를 잠깐 무시하겠다는 뜻.
        // 자바가 형변환을 안전한거 맞냐라고 경고할 수 있기 때문.
        @SuppressWarnings("unchecked")
        List<Number> result = entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword)
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
                    OR MATCH(u.description) AGAINST (:keyword IN BOOLEAN MODE)
                  )
                """;

        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword)
                .getSingleResult();

        return count.longValue();
    }

    // 이름 검색용 LIKE 조건.
    // 이름으로 검색은 FULLTEXT로 처리가 불가능하기 때문이다.
    private BooleanExpression nameLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return QUser.user.name.containsIgnoreCase(keyword);
    }

    // 하이브리드 분기 기준.
    // 검색어 안에 공백이 포함되어 있는지 확인.
    private boolean hasWhitespace(String keyword) {
        return keyword != null && keyword.contains(" ");
    }
}



