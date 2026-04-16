package com.example.readys7project.domain.project.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.search.dto.response.ProjectsTotalSearchResponseDto;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.readys7project.domain.project.entity.QProject.project;

@Repository
@RequiredArgsConstructor
public class ProjectQueryRepositoryImpl implements ProjectQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<Project> searchProjects(
            String keyword,
            Category category,
            ProjectStatus status,
            List<String> skills,
            Pageable pageable
    ) {
        QProject qProject = project;

        // 동적 조건 빌더 - 각 조건이 null이면 자동으로 무시됨
        BooleanBuilder builder = new BooleanBuilder();

        // 키워드 조건 (제목 or 설명)
        if (keyword != null && !keyword.isBlank()) {
            builder.and(qProject.title.containsIgnoreCase(keyword)
                    .or(qProject.description.containsIgnoreCase(keyword)));
        }

        // 카테고리 조건 (null이면 전체 조회)
        if (category != null) {
            builder.and(qProject.category.eq(category));
        }

        // 상태 조건 (null이면 전체 조회)
        if (status != null) {
            builder.and(qProject.status.eq(status));
        }

        // 스킬 조건
        if (skills != null && !skills.isEmpty()) {
            // OR 조건들을 담을 별도의 빌더 생성
            // ex) "Java" OR "Python" OR "Spring" 형태로 조건을 쌓기 위함
            BooleanBuilder skillBuilder = new BooleanBuilder();

            for (String skill : skills) {
                skillBuilder.or(
                        // QueryDSL이 기본 지원하지 않는 MySQL 전용 JSON 함수를 사용하기 위해
                        // booleanTemplate으로 네이티브 SQL 함수를 문자열 템플릿 형태로 직접 주입
                        Expressions.booleanTemplate(
                                // JSON_CONTAINS : JSON 배열 안에 특정 값이 원소로 정확히 존재하는지 검사 (true/false 반환)
                                // JSON_QUOTE    : 일반 문자열을 JSON 문자열 형식으로 변환 ("Java" → '"Java"')
                                //                 이 함수가 없으면 MySQL이 'Java'를 JSON으로 파싱하려다 오동작 발생
                                // {0}           : 첫 번째 인자 자리 → DB의 skills JSON 컬럼
                                // {1}           : 두 번째 인자 자리 → 사용자가 검색 조건으로 입력한 skill 문자열
                                "JSON_CONTAINS({0}, JSON_QUOTE({1}))",
                                qProject.skills, // {0} : skills JSON 컬럼 바인딩
                                skill             // {1} : 검색할 스킬 문자열 바인딩
                        )
                );
            }
            // 완성된 OR 조건 묶음을 메인 빌더에 AND 조건으로 추가
            // ex) category = 'IT' AND (skills에 'Java' 포함 OR skills에 'Python' 포함)
            builder.and(skillBuilder);
        }

        // 실제 데이터 조회 쿼리 (fetchJoin 추가하여 N+1 방지)
        List<Project> content = jpaQueryFactory
                .selectFrom(qProject)
                .leftJoin(qProject.category).fetchJoin()
                .leftJoin(qProject.client).fetchJoin()
                .leftJoin(qProject.client.user).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())       // 페이지 시작점
                .limit(pageable.getPageSize())      // 페이지 크기
                .orderBy(qProject.createdAt.desc()) // 최신순 정렬
                .fetch();

        // 전체 카운트 쿼리 (페이징 메타데이터용)
        // count 쿼리는 별도로 분리하여 불필요한 JOIN 방지
        Long total = jpaQueryFactory
                .select(qProject.count())
                .from(qProject)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Project> findByClientWithPageable(Long clientId, Pageable pageable) {

        QProject qProject = project;
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


    // 통합 검색 페이징
    @Override
    public Page<ProjectsTotalSearchResponseDto> projectsTotalSearch(String keyword, Pageable pageable) {

        // 1. 데이터 조회

        // ProjectPopularSearchResponseDto에 정의된 필요한 5개의 필드만 가져옴
        // Project 내부의 기본 필드들만 가져오기 때문에 다른 테이블 조회를 하지 않음 -> Fetch Join 안써도 N+1 발생 X
        List<ProjectsTotalSearchResponseDto> content = jpaQueryFactory
                .select(Projections.constructor(ProjectsTotalSearchResponseDto.class,
                        project.id,
                        project.title,
                        project.minBudget,
                        project.maxBudget,
                        project.status
                ))
                .from(project)
                .where(searchAllCondition(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(project.createdAt.desc())
                .fetch();

        // 카운트 쿼리 분리
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(project.count())
                .from(project)
                .where(searchAllCondition(keyword));

        // PageableExecutionUtils를 사용하여 최적화된 Page 객체 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

        /* countQuery를 실행할 때 .fetchOne()을 바로 붙이지 않는 이유는
         .fetchOne()을 실행하기 전까지는 실제로 DB에 쿼리가 날아가지 않음 -> 지연로딩

         PageableExecutionUtils.getPage()
         -> 카운트 쿼리를 실행하지 않아도 전체 개수를 알수 있는 상황을 감지해서 성능을 아껴주는 역할

         countQuery::fetchOne -> 메서드 참조, 당장 쿼리를 실행하는 것이 아닌, 개수 정보가 진짜로 필요해지면
         그 때 해당 메서드를 호출해서 값을 가져가라는 의미

         PageImpl 방식은 무조건 DB에서 total을 미리 조회해 와야함, 항상 2번의 쿼리가 나감
         PageableExecutionUtils를 사용하면 특정 상황에서 카운트 쿼리를 생략할 수 있어서 DB 리소스를 아낄 수 있음
         */
    }

    // 동적 쿼리 Predicate 활용

    // Predicate란? -> Predicate는 최상위 인터페이스,
    // BooleanExpression과 BooleanBuilder는 Predicate를 구현한 추상 클래스
    // BooleanExpression과 BooleanBuilder를 섞어서 활용하므로, 반환 타입을 Predicate로 맞춤

    private Predicate searchAllCondition(String keyword) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }

        // keyword값 미리 trim처리
        String trimmedKeyword = keyword.trim();

        // NPE 방지를 위해 BooleanBuilder 사용,
        // BooleanExpression에서는 호출하는 객체가 null이면 바로 NPE가 발생함
        BooleanBuilder builder = new BooleanBuilder();
        // 검색 조건이 4개나 되기 때문에, null이 들어오면 BooleanBuilder가 내부적으로 null을 무시함
        builder.or(titleLike(trimmedKeyword));
        builder.or(descriptionLike(trimmedKeyword));
        builder.or(skillsLike(trimmedKeyword));
        builder.or(categoryNameLike(trimmedKeyword));

        return builder.getValue();
    }

    // 가독성 -> BooleanExpression
    // 조건의 조합을 안전하게 처리 -> BooleanBuilder


    // title 검색 조건
    private BooleanExpression titleLike(String keyword) {
        // 키워드가 비어있으면 조건을 거지 않고 null 반환 (null이면 where절에서 자동으로 무시됨)
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        // 대소문자 무시하고 키워드 포함
        return project.title.containsIgnoreCase(keyword);
    }

    // description 검색 조건
    private BooleanExpression descriptionLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return project.description.containsIgnoreCase(keyword);
    }

    // skill 검색 조건
    private BooleanExpression skillsLike(String keyword) {
        if(keyword == null || keyword.trim().isEmpty()){
            return null;
        }
        return Expressions.booleanTemplate(
                "CAST(JSON_CONTAINS({0}, JSON_QUOTE({1})) AS integer) = 1",
                project.skills,
                keyword
        );
    }
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
    private BooleanExpression categoryNameLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return project.category.name.containsIgnoreCase(keyword);
    }

}
