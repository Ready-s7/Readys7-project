package com.example.readys7project.domain.project.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
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

    @Override
    public Page<Project> searchProjects(
            Category category,
            ProjectStatus status,
            List<String> skills,
            Pageable pageable
    ) {
        QProject qProject = QProject.project;

        // 동적 조건 빌더 - 각 조건이 null이면 자동으로 무시됨
        BooleanBuilder builder = new BooleanBuilder();

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

        // 실제 데이터 조회 쿼리
        List<Project> content = jpaQueryFactory
                .selectFrom(qProject)
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

        QProject qProject = QProject.project;
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
}
