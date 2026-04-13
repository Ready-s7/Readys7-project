package com.example.readys7project.domain.skill.repository;

import com.example.readys7project.domain.skill.entity.QSkill;
import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.user.admin.entity.QAdmin;
import com.example.readys7project.domain.user.auth.entity.QUser;
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
public class SkillQueryRepositoryImpl implements SkillQueryRepository{

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Page<Skill> findByNameAndCategory(String name, SkillCategory category, Pageable pageable) {
        QSkill qSkill = QSkill.skill;
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
        QSkill qSkill = QSkill.skill;
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
}
