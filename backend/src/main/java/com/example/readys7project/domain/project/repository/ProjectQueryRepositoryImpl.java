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
import com.querydsl.core.types.Predicate;
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

    // 하이브리드 전략의 핵심.
    // 통합검색에서 프로젝트 검색읋 할 때 검색어에 공백이 있으면 FULLTEXT
    // 없으면 LIKE
    // 왜냐면 FULLTEXT는 단일 키워드를 제대로 조회 못할 확률이 높다.

    @Override
    public SearchPageResponseDto<ProjectsGlobalSearchResponseDto> projectsGlobalSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }

        // 검색어 양끝 공백을 제거한 새 문자열을 변수에 담는 코드
        // " 백엔드 개발 " -> "백엔드 개발"
        String trimmedKeyword = keyword.trim();

        if (hasWhitespace(trimmedKeyword)) {
            return projectsGlobalSearchByFullText(keyword, pageable);
        }

        return projectsGlobalSearchByLike(trimmedKeyword, pageable);
    }

    /**
     하이브리드에서는 LIKE 로직, FULLTEXT 로직을 분리.
     */
    // LIKE 전용 프로젝트 검색
    // 공백 없는 검색어를 처리하는 메서드
    private SearchPageResponseDto<ProjectsGlobalSearchResponseDto> projectsGlobalSearchByLike(String keyword, Pageable pageable) {

        QCategory qCategory = QCategory.category;
        QClient qClient = QClient.client;
        QUser qUser = QUser.user;

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
                .where(searchAllCondition(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qProject.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qProject.count())
                .from(qProject)
                .leftJoin(qProject.category, QCategory.category)
                .where(searchAllCondition(keyword));

        Page<ProjectsGlobalSearchResponseDto> page = PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

        return SearchPageResponseDto.from(page);
    }

    // FULLTEXT 전용 프로젝트 검색
    // 공백 있는 복합 검색어를 처리하는 메서드
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

    // 프로젝트 LIKE 검색에서 “검색 조건 전체를 만드는 함수”.
    // 즉 사용자가 검색어를 넣었을 때,
    // 그 검색어를 기준으로 제목, 설명, 스킬, 카테고리 중 하나라도 맞으면 검색되도록 만든다.
    // Predicate는 함수가 아니라 QueryDSL에서 “검색 조건”을 표현하는 공통 타입(인터페이스)이다.
    // 즉:BooleanExpression도 조건, BooleanBuilder도 조건. 이 둘을 모두 받아줄 상위 타입이 Predicate이다.
    private Predicate searchAllCondition(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();

        BooleanBuilder builder = new BooleanBuilder();
        builder.or(titleLike(trimmedKeyword));
        builder.or(descriptionLike(trimmedKeyword));
        builder.or(skillsLike(trimmedKeyword));
        builder.or(categoryNameLike(trimmedKeyword));

        return builder.getValue();
    }

    // 제목 Like 검색
    private BooleanExpression titleLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return qProject.title.containsIgnoreCase(keyword);
    }

    // 설명 Like 검색
    private BooleanExpression descriptionLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return qProject.description.containsIgnoreCase(keyword);
    }

    private BooleanExpression skillsLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return Expressions.booleanTemplate(
                "CAST(JSON_CONTAINS({0}, JSON_QUOTE({1})) AS integer) = 1",
                qProject.skills,
                keyword
        );
    }

    // 카테고리 이름도 검색
    private BooleanExpression categoryNameLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return qProject.category.name.containsIgnoreCase(keyword);
    }

    // MATCH ... AGAINST 같은 MySQL FULLTEXT 문법은
    // QueryDSL/JPQL에서 깔끔하게 처리하기 어렵기 때문에, native SQL로 분리
    // FULLTEXT 검색으로 프로젝트 ID 목록을 찾는 함수
    private List<Long> findProjectIdsByFullText(String keyword, Pageable pageable) {
        // 실제로 DB에 보낼 native SQL.
        // native SQL은 JPA나 QueryDSL 문법이 아니라, DB에 직접 보내는 “진짜 SQL 문장”을 뜻한다.
        String sql = """
                SELECT p.id
                FROM projects p
                WHERE p.is_deleted = false
                  AND MATCH(p.title, p.description) AGAINST (:keyword IN BOOLEAN MODE)
                ORDER BY p.created_at DESC
                LIMIT :limit OFFSET :offset
                """;
        // AND MATCH(p.title, p.description) AGAINST (:keyword IN BOOLEAN MODE)
        // 핵심
        // MATCH(p.title, p.description)
        // title, description 컬럼을 검색 대상으로 지정
        // AGAINST(:keyword IN BOOLEAN MODE)
        // keyword를 검색어로 해서 FULLTEXT 검색 수행


        // 컴파일러 경고를 잠깐 무시하겠다는 뜻.
        // 자바가 형변환을 안전한거 맞냐라고 경고할 수 있기 때문.
        @SuppressWarnings("unchecked")
        // EntityManager는 DB 작업을 실제로 실행해주는 관리자. 직접 DB에 대화하는 객체이다.
        // List<Number> 이유. DB에서 id를 가져오면 JPA가 보통 숫자 타입으로 받기 때문.
        // 직접 Long으로 오늘게 아니라 Number로 받아서 나중에 변환. 589줄에서 변환
        // .setParameter()는 SQL 안에 있는 변수 자리에 실제 값을 넣는 것
        List<Number> result = entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword)
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", pageable.getOffset())
                .getResultList();

        return result.stream()
                .map(Number::longValue)
                .toList();
    }

    // 이 메서드는 FULLTEXT 검색 결과가 총 몇 개인지 세는 함수
    // 즉, 프로젝트를 실제로 조회할 때는 현재 페이지에 보여줄 데이터만 가져오고,
    // 이 메서드는 전체 검색 결과 개수를 구해서 페이징에 쓰는 역할
    private long countProjectsByFullText(String keyword) {
        String sql = """
                SELECT COUNT(*)
                FROM projects p
                WHERE p.is_deleted = false
                  AND MATCH(p.title, p.description) AGAINST (:keyword IN BOOLEAN MODE)
                """;

        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword)
                .getSingleResult();

        return count.longValue();
    }

    // 중요한 분기 기준.
    // 검색어에 공백이 있는지 확인.
    // 왜냐면 검색어에 공백이 있어야. LIKE문과 FULLTEXT 분기 기준이다.
    private boolean hasWhitespace(String keyword) {
        return keyword != null && keyword.contains(" ");
    }

}


