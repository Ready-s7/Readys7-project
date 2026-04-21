package com.example.readys7project.domain.skill.repository;

import com.example.readys7project.domain.search.dto.response.SearchPageResponseDto;
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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

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

    // 통합 검색 페이징 (N-gram Full-text 적용)
    @Override
    public SearchPageResponseDto<SkillsGlobalSearchResponseDto> skillsGlobalSearch(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isBlank()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();
        QAdmin qAdmin = QAdmin.admin;
        QUser qUser = QUser.user;

        List<Long> skillIds = findSkillIdsByFullText(trimmedKeyword, pageable);

        if (skillIds.isEmpty()) {
            return SearchPageResponseDto.from(Page.empty(pageable));
        }

        // 데이터 조회
        List<SkillsGlobalSearchResponseDto> content = jpaQueryFactory
                .select(Projections.constructor(SkillsGlobalSearchResponseDto.class,
                        qSkill.id,
                        qAdmin.id,
                        qUser.name,
                        qSkill.name,
                        qSkill.skillCategory,
                        qSkill.createdAt
                ))
                .from(qSkill)
                .leftJoin(qSkill.admin, qAdmin)
                .leftJoin(qAdmin.user, qUser)
                .where(qSkill.id.in(skillIds))
                .orderBy(qSkill.name.asc()) // 가,나,다 순 알파벳 정렬
                .fetch();

        long total = countSkillsByFullText(trimmedKeyword);
        Page<SkillsGlobalSearchResponseDto> page = new org.springframework.data.domain.PageImpl<>(content, pageable, total);

        return SearchPageResponseDto.from(page);
    }

    private List<Long> findSkillIdsByFullText(String keyword, Pageable pageable) {
        String sql = """
                SELECT s.id
                FROM skills s
                WHERE (
                    MATCH(s.name) AGAINST (:keyword IN BOOLEAN MODE)
                    OR s.name LIKE CONCAT('%', :keyword, '%')
                )
                ORDER BY s.name ASC
                LIMIT :limit OFFSET :offset
                """;

        @SuppressWarnings("unchecked")
        List<Number> result = entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword) // + 제거
                .setParameter("limit", pageable.getPageSize())
                .setParameter("offset", pageable.getOffset())
                .getResultList();

        return result.stream().map(Number::longValue).toList();
    }

    private long countSkillsByFullText(String keyword) {
        String sql = """
                SELECT COUNT(*) FROM skills s 
                WHERE (
                    MATCH(s.name) AGAINST (:keyword IN BOOLEAN MODE)
                    OR s.name LIKE CONCAT('%', :keyword, '%')
                )
                """;
        Number count = (Number) entityManager.createNativeQuery(sql)
                .setParameter("keyword", keyword) // + 제거
                .getSingleResult();
        return count.longValue();
    }
}
