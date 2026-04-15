package com.example.readys7project.domain.skill.repository;

import com.example.readys7project.domain.search.dto.response.SkillPopularSearchResponseDto;
import com.example.readys7project.domain.skill.entity.QSkill;
import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.user.admin.entity.QAdmin;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.readys7project.domain.skill.entity.QSkill.skill;

@Repository
@RequiredArgsConstructor
public class SkillQueryRepositoryImpl implements SkillQueryRepository{

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Page<Skill> findByNameAndCategory(String name, SkillCategory category, Pageable pageable) {
        QSkill qSkill = skill;
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
        QSkill qSkill = skill;
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

    // 인기 검색 페이징
    @Override
    public Page<SkillPopularSearchResponseDto> skillsPopularSearch(String keyword, Pageable pageable) {

        // 데이터 조회
        List<SkillPopularSearchResponseDto> content = jpaQueryFactory
                .select(Projections.constructor(SkillPopularSearchResponseDto.class,
                        skill.id,
                        skill.name,
                        skill.skillCategory
                ))
                .from(skill)
                .where(nameLike(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(skill.name.asc()) // 가,나,다 순 알파벳 정렬
                .fetch();

        // 전체 데이터 개수 조회
        Long total = jpaQueryFactory
                .select(skill.count())
                .from(skill)
                .where(nameLike(keyword))
                .fetchOne();

        // 데이터 개수 조회 결과가 없을 수도 있으니, 방어코드
        long totalCount = total != null ? total : 0L;

        // PageImpl로 감싸서 반환
        return new PageImpl<>(content, pageable, totalCount);
    }

    // BooleanExpression (where 절에 활용)
    private BooleanExpression nameLike(String keyword) {
        // 검색 키워드가 없으면 그냥 리턴해주고, 포함되어있으면 해당 키워드 리턴해줌
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return skill.name.contains(keyword);
    }
}
