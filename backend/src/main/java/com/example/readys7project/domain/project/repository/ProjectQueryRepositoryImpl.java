package com.example.readys7project.domain.project.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.search.dto.response.ProjectsGlobalSearchResponseDto;
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

//    @Override
//    public Page<Project> searchProjects(
//            String keyword,
//            Category category,
//            ProjectStatus status,
//            List<String> skills,
//            Pageable pageable
//    ) {
//
//        // 동적 조건 빌더 - 각 조건이 null이면 자동으로 무시됨
//        BooleanBuilder builder = new BooleanBuilder();
//
//        // 키워드 조건 (제목 or 설명)
//        if (keyword != null && !keyword.isBlank()) {
//            builder.and(qProject.title.containsIgnoreCase(keyword)
//                    .or(qProject.description.containsIgnoreCase(keyword)));
//        }
//
//        // 카테고리 조건 (null이면 전체 조회)
//        if (category != null) {
//            builder.and(qProject.category.eq(category));
//        }
//
//        // 상태 조건 (null이면 전체 조회)
//        if (status != null) {
//            builder.and(qProject.status.eq(status));
//        }
//
//        // 스킬 조건
//        if (skills != null && !skills.isEmpty()) {
//            // OR 조건들을 담을 별도의 빌더 생성
//            // ex) "Java" OR "Python" OR "Spring" 형태로 조건을 쌓기 위함
//            BooleanBuilder skillBuilder = new BooleanBuilder();
//
//            for (String skill : skills) {
//                skillBuilder.or(
//                        // QueryDSL이 기본 지원하지 않는 MySQL 전용 JSON 함수를 사용하기 위해
//                        // booleanTemplate으로 네이티브 SQL 함수를 문자열 템플릿 형태로 직접 주입
//                        Expressions.booleanTemplate(
//                                // JSON_CONTAINS : JSON 배열 안에 특정 값이 원소로 정확히 존재하는지 검사 (true/false 반환)
//                                // JSON_QUOTE    : 일반 문자열을 JSON 문자열 형식으로 변환 ("Java" → '"Java"')
//                                //                 이 함수가 없으면 MySQL이 'Java'를 JSON으로 파싱하려다 오동작 발생
//                                // {0}           : 첫 번째 인자 자리 → DB의 skills JSON 컬럼
//                                // {1}           : 두 번째 인자 자리 → 사용자가 검색 조건으로 입력한 skill 문자열
//                                "JSON_CONTAINS({0}, JSON_QUOTE({1}))",
//                                qProject.skills, // {0} : skills JSON 컬럼 바인딩
//                                skill             // {1} : 검색할 스킬 문자열 바인딩
//                        )
//                );
//            }
//            // 완성된 OR 조건 묶음을 메인 빌더에 AND 조건으로 추가
//            // ex) category = 'IT' AND (skills에 'Java' 포함 OR skills에 'Python' 포함)
//            builder.and(skillBuilder);
//        }
//
//        // 실제 데이터 조회 쿼리 (fetchJoin 추가하여 N+1 방지)
//        List<Project> content = jpaQueryFactory
//                .selectFrom(qProject)
//                .leftJoin(qProject.category).fetchJoin()
//                .leftJoin(qProject.client).fetchJoin()
//                .leftJoin(qProject.client.user).fetchJoin()
//                .where(builder)
//                .offset(pageable.getOffset())       // 페이지 시작점
//                .limit(pageable.getPageSize())      // 페이지 크기
//                .orderBy(qProject.createdAt.desc()) // 최신순 정렬
//                .fetch();
//
//        // 전체 카운트 쿼리 (페이징 메타데이터용)
//        // count 쿼리는 별도로 분리하여 불필요한 JOIN 방지
//        Long total = jpaQueryFactory
//                .select(qProject.count())
//                .from(qProject)
//                .where(builder)
//                .fetchOne();
//
//        return new PageImpl<>(content, pageable, total != null ? total : 0);
//    }
//
//    @Override
//    public Page<Project> findByClientWithPageable(Long clientId, Pageable pageable) {
//
//        QClient qClient = QClient.client;
//        QUser qUser = QUser.user;
//
//        List<Project> content = jpaQueryFactory
//                .selectFrom(qProject)
//                .join(qProject.client, qClient).fetchJoin()
//                .join(qClient.user, qUser).fetchJoin()
//                .where(qProject.client.id.eq(clientId))
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
//                .orderBy(qProject.createdAt.desc())
//                .fetch();
//
//        Long total = jpaQueryFactory
//                .select(qProject.count())
//                .from(qProject)
//                .where(qProject.client.id.eq(clientId))
//                .fetchOne();
//
//        return new PageImpl<>(content, pageable, total != null ? total : 0);
//
//    }
//
//
//    // 통합 검색 페이징
//    @Override
//    public Page<ProjectsGlobalSearchResponseDto> projectsGlobalSearch(String keyword, Pageable pageable) {
//
//
//        // 1. 데이터 조회
//
//        // ProjectPopularSearchResponseDto에 정의된 필요한 5개의 필드만 가져옴
//        // Project 내부의 기본 필드들만 가져오기 때문에 다른 테이블 조회를 하지 않음 -> Fetch Join 안써도 N+1 발생 X
//        List<ProjectsGlobalSearchResponseDto> content = jpaQueryFactory
//                .select(Projections.constructor(ProjectsGlobalSearchResponseDto.class,
//                        qProject.id,
//                        qProject.title,
//                        qProject.description,
//                        qProject.category.name,
//                        qProject.minBudget,
//                        qProject.maxBudget,
//                        qProject.duration,
//                        qProject.skills,
//                        qProject.status.stringValue(),
//                        qProject.currentProposalCount,
//                        qProject.maxProposalCount,
//                        qProject.client.user.name,
//                        qProject.client.rating,
//                        qProject.createdAt,
//                        qProject.updatedAt
//                ))
//                .from(qProject)
//                .leftJoin(qProject.category)
//                .leftJoin(qProject.client)
//                .leftJoin(qProject.client.user)
//                .where(searchAllCondition(keyword))
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
//                .orderBy(qProject.createdAt.desc())
//                .fetch();
//
//        // 카운트 쿼리 분리
//        JPAQuery<Long> countQuery = jpaQueryFactory
//                .select(qProject.count())
//                .from(qProject)
//                .where(searchAllCondition(keyword));
//
//        // PageableExecutionUtils를 사용하여 최적화된 Page 객체 반환
//        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
//
//        /* countQuery를 실행할 때 .fetchOne()을 바로 붙이지 않는 이유는
//         .fetchOne()을 실행하기 전까지는 실제로 DB에 쿼리가 날아가지 않음 -> 지연로딩
//
//         PageableExecutionUtils.getPage()
//         -> 카운트 쿼리를 실행하지 않아도 전체 개수를 알수 있는 상황을 감지해서 성능을 아껴주는 역할
//
//         countQuery::fetchOne -> 메서드 참조, 당장 쿼리를 실행하는 것이 아닌, 개수 정보가 진짜로 필요해지면
//         그 때 해당 메서드를 호출해서 값을 가져가라는 의미
//
//         PageImpl 방식은 무조건 DB에서 total을 미리 조회해 와야함, 항상 2번의 쿼리가 나감
//         PageableExecutionUtils를 사용하면 특정 상황에서 카운트 쿼리를 생략할 수 있어서 DB 리소스를 아낄 수 있음
//         */
//    }
//
//    // 동적 쿼리 Predicate 활용
//
//    // Predicate란? -> Predicate는 최상위 인터페이스,
//    // BooleanExpression과 BooleanBuilder는 Predicate를 구현한 추상 클래스
//    // BooleanExpression과 BooleanBuilder를 섞어서 활용하므로, 반환 타입을 Predicate로 맞춤
//
//    private Predicate searchAllCondition(String keyword) {
//        if (keyword == null || keyword.trim().isBlank()) {
//            return null;
//        }
//
//        // keyword값 미리 trim처리
//        String trimmedKeyword = keyword.trim();
//
//        // NPE 방지를 위해 BooleanBuilder 사용,
//        // BooleanExpression에서는 호출하는 객체가 null이면 바로 NPE가 발생함
//        BooleanBuilder builder = new BooleanBuilder();
//        // 검색 조건이 4개나 되기 때문에, null이 들어오면 BooleanBuilder가 내부적으로 null을 무시함
//        builder.or(titleLike(trimmedKeyword));
//        builder.or(descriptionLike(trimmedKeyword));
//        builder.or(skillsLike(trimmedKeyword));
//        builder.or(categoryNameLike(trimmedKeyword));
//
//        return builder.getValue();
//    }
//
//    // 가독성 -> BooleanExpression
//    // 조건의 조합을 안전하게 처리 -> BooleanBuilder
//
//
//    // title 검색 조건
//    private BooleanExpression titleLike(String keyword) {
//        // 키워드가 비어있으면 조건을 거지 않고 null 반환 (null이면 where절에서 자동으로 무시됨)
//        if (keyword == null || keyword.trim().isEmpty()) {
//            return null;
//        }
//        // 대소문자 무시하고 키워드 포함
//        return qProject.title.containsIgnoreCase(keyword);
//    }
//
//    // description 검색 조건
//    private BooleanExpression descriptionLike(String keyword) {
//        if (keyword == null || keyword.trim().isEmpty()) {
//            return null;
//        }
//        return qProject.description.containsIgnoreCase(keyword);
//    }
//
//    // skill 검색 조건
//    private BooleanExpression skillsLike(String keyword) {
//        if(keyword == null || keyword.trim().isEmpty()){
//            return null;
//        }
//        return Expressions.booleanTemplate(
//                "CAST(JSON_CONTAINS({0}, JSON_QUOTE({1})) AS integer) = 1",
//                qProject.skills,
//                keyword
//        );
//    }
    /* Expressions.booleanTemplate(
                    "CAST(JSON_CONTAINS({0}, JSON_QUOTE({1})) AS integer) = 1",
                    project.skills,
                    keyword
     -> QueryDSL은 자바 코드로 모든 SQL을 지원하려고 노력하지만, 특정 DB (MySQL)의 특수한 함수까지는
     다 알지 못하기 때문에 사용하는 방식

     Expressions.booleanTemplate -> 직접 SQL 문법을 작성할 테니, 결과값은 true/false로 처리해주라는 의미
     "CAST(JSON_CONTAINS({0}, JSON_QUOTE({1})) AS integer) = 1"
     -> {0}, {1} 뒤에 오는 인자, project.skills, keyword가 순서대로 들어갈 자리 표시자,

     JSON_CONTAINS({0} -> 첫번째 인자,({0}, 즉 project.skills) JSON 배열 안에 두번째 인자가 포함되어 있는지
     확인하는 MySQL 전용 함수

     JSON_QUOTE({1}) -> 일반 문자열을 JSON 형식의 문자열로 변환해주는 역할

     CAST(...) = 1 -> 결과 값을 명확하게 정수형(Integer)로 변환하여 1과 비교하는 역할
     booleanTemplate는 true/false를 반환하는데, JSON_CONTAINS의 결과인 0 / 1을 자바의 true/false로 확실하게 매핑하기 위해
     결과가 1과 같은지 확인하는 조건

     skill만 이렇게 조건을 만드는 이유는, @Convert를 통해 DB에 JSON 형태로 저장되기 때문이다
     ex) ["Java", "Spring", "React"]
     일반 텍스트는 LIKE 검색이 기본이지만, JSON 배열 형태로 저장된 데이터는 DB가 내부적으로
     "이건 단순 글자가 아니라 리스트야" 라고 인식하고 있기 때문에 사용

     */

    // category 검색 조건
//    private BooleanExpression categoryNameLike(String keyword) {
//        if (keyword == null || keyword.trim().isEmpty()) {
//            return null;
//        }
//        return qProject.category.name.containsIgnoreCase(keyword);
//    }


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
    public Page<ProjectsGlobalSearchResponseDto> projectsGlobalSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isBlank()) {
            return Page.empty(pageable);
        }

        // 검색어 양끝 공백을 제거한 새 문자열을 변수에 담는 코드
        // " 백엔드 개발 " -> "백엔드 개발"
        String trimmedKeyword = keyword.trim();

        if (hasWhitespace(trimmedKeyword)) {
            return projectsGlobalSearchByFullText(trimmedKeyword, pageable);
        }

        return projectsGlobalSearchByLike(trimmedKeyword, pageable);
    }

    /**
     하이브리드에서는 LIKE 로직, FULLTEXT 로직을 분리.
     */
    // LIKE 전용 프로젝트 검색
    // 공백 없는 검색어를 처리하는 메서드
    private Page<ProjectsGlobalSearchResponseDto> projectsGlobalSearchByLike(String keyword, Pageable pageable) {
        List<ProjectsGlobalSearchResponseDto> content = jpaQueryFactory
                .select(Projections.constructor(ProjectsGlobalSearchResponseDto.class,
                        qProject.id,
                        qProject.title,
                        qProject.description,
                        qProject.category.name,
                        qProject.minBudget,
                        qProject.maxBudget,
                        qProject.duration,
                        qProject.skills,
                        qProject.status.stringValue(),
                        qProject.currentProposalCount,
                        qProject.maxProposalCount,
                        qProject.client.user.name,
                        qProject.client.rating,
                        qProject.createdAt,
                        qProject.updatedAt
                ))
                .from(qProject)
                .leftJoin(qProject.category)
                .leftJoin(qProject.client)
                .leftJoin(qProject.client.user)
                .where(searchAllCondition(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qProject.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qProject.count())
                .from(qProject)
                .where(searchAllCondition(keyword));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // FULLTEXT 전용 프로젝트 검색
    // 공백 있는 복합 검색어를 처리하는 메서드
    private Page<ProjectsGlobalSearchResponseDto> projectsGlobalSearchByFullText(String keyword, Pageable pageable) {
        List<Long> projectIds = findProjectIdsByFullText(keyword, pageable);

        if (projectIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<ProjectsGlobalSearchResponseDto> content = jpaQueryFactory
                .select(Projections.constructor(ProjectsGlobalSearchResponseDto.class,
                        qProject.id,
                        qProject.title,
                        qProject.description,
                        qProject.category.name,
                        qProject.minBudget,
                        qProject.maxBudget,
                        qProject.duration,
                        qProject.skills,
                        qProject.status.stringValue(),
                        qProject.currentProposalCount,
                        qProject.maxProposalCount,
                        qProject.client.user.name,
                        qProject.client.rating,
                        qProject.createdAt,
                        qProject.updatedAt
                ))
                .from(qProject)
                .leftJoin(qProject.category)
                .leftJoin(qProject.client)
                .leftJoin(qProject.client.user)
                .where(qProject.id.in(projectIds))
                .orderBy(qProject.createdAt.desc())
                .fetch();

        long total = countProjectsByFullText(keyword);

        return new PageImpl<>(content, pageable, total);
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


