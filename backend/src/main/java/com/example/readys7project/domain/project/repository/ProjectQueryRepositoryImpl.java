package com.example.readys7project.domain.project.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.querydsl.core.BooleanBuilder;
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

        // 스킬 조건 - OR 조건 (skills 중 하나라도 JSON 문자열에 포함되면 조회)
        // skills 필드가 JSON 문자열이므로 LIKE로 각 스킬을 검색
        if (skills != null && !skills.isEmpty()) {
            BooleanBuilder skillBuilder = new BooleanBuilder();
            for (String skill : skills) {
                // JSON 문자열 안에 해당 스킬이 포함되어 있는지 LIKE 검색
                // 예: skills = ["Java", "Python"] 에서 "Java" 검색 시
                // LIKE '%Java%' 로 검색
                skillBuilder.or(qProject.skills.contains(skill));
            }
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
