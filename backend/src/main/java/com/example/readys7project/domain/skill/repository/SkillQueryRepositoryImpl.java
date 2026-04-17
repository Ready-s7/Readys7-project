package com.example.readys7project.domain.skill.repository;

import com.example.readys7project.domain.search.dto.response.SkillsGlobalSearchResponseDto;
import com.example.readys7project.domain.skill.entity.QSkill;
import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.user.admin.entity.QAdmin;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@RequiredArgsConstructor
public class SkillQueryRepositoryImpl implements SkillQueryRepository{

    private final JPAQueryFactory jpaQueryFactory;

    QSkill qSkill = QSkill.skill;

    @Override
    public Page<Skill> findByNameAndCategory(String name, SkillCategory category, Pageable pageable) {
        QAdmin qAdmin = QAdmin.admin;
        QUser qAdminUser = new QUser("adminUser");

        BooleanBuilder builder = new BooleanBuilder();

        // 네임 조건
        if (name != null && !name.isBlank()) {
            builder.and(qSkill.name.contains(name));
        }

        // 카테고리 조건
        if (category != null) {
            builder.and(qSkill.skillCategory.eq(category));
        }

        List<Skill> content = jpaQueryFactory
                .selectFrom(qSkill)
                .join(qSkill.admin, qAdmin).fetchJoin()
                .join(qAdmin.user, qAdminUser).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(qSkill.count())
                .from(qSkill)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<Skill> findAllWithAdminAndUser(Pageable pageable) {
        QAdmin qAdmin = QAdmin.admin;
        QUser qAdminUser = new QUser("adminUser");

        List<Skill> content = jpaQueryFactory
                .selectFrom(qSkill)
                .join(qSkill.admin, qAdmin).fetchJoin()
                .join(qAdmin.user, qAdminUser).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = jpaQueryFactory
                .select(qSkill.count())
                .from(qSkill)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 통합 검색 페이징
    @Override
    public Page<SkillsGlobalSearchResponseDto> skillsGlobalSearch(String keyword, Pageable pageable) {

        // 데이터 조회
        List<SkillsGlobalSearchResponseDto> content = jpaQueryFactory
                .select(Projections.constructor(SkillsGlobalSearchResponseDto.class,
                        qSkill.id,
                        qSkill.admin.id,
                        qSkill.admin.user.name,
                        qSkill.name,
                        qSkill.skillCategory,
                        qSkill.createdAt
                ))
                .from(qSkill)
                .where(searchCondition(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qSkill.name.asc()) // 가,나,다 순 알파벳 정렬
                .fetch();

        // 카운트 쿼리 분리 (지연 로딩)
        JPAQuery<Long> contQuery = jpaQueryFactory
                .select(qSkill.count())
                .from(qSkill)
                .where(searchCondition(keyword));

        return PageableExecutionUtils.getPage(content, pageable, contQuery::fetchOne);
    }

    private Predicate searchCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        // 미리 trim처리
        String trimmedKeyword = keyword.trim();

        BooleanBuilder builder = new BooleanBuilder();

        // 기술 이름 검색 (Java, React 등등)
        builder.or(nameLike(trimmedKeyword));
        // 카테고리 이름 검색 (Backend, Frontend 등등)
        builder.or(categoryNameLike(trimmedKeyword));

        return builder.getValue();

    }

    // Skill 이름 검색 조건
    private BooleanExpression nameLike(String keyword) {
        // 검색 키워드가 없으면 그냥 리턴해주고, 포함되어있으면 해당 키워드 리턴해줌
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return qSkill.name.containsIgnoreCase(keyword);
    }

    // SkillCategory 검색 조건
    private BooleanExpression categoryNameLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        // stringValue -> Enum 타입을 String으로 변환환해줌
        // Enum 타입의 SkillCategory를 검색 조건에 포함시키기 위해 stringValue()로 변환
        return qSkill.skillCategory.stringValue().containsIgnoreCase(keyword);
    }
}
