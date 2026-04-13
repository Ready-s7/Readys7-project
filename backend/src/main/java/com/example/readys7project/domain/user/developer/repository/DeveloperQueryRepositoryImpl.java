package com.example.readys7project.domain.user.developer.repository;

import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.entity.QDeveloper;
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
public class DeveloperQueryRepositoryImpl implements DeveloperQueryRepository {

    private final JPAQueryFactory queryFactory;

    // 전체 개발자 목록 조회
    @Override
    public Page<Developer> findAllWithUser(Pageable pageable) {
        QDeveloper qDeveloper = QDeveloper.developer;
        QUser qUser = QUser.user;

        // 실제 데이터 조회
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

    // 개발자 검색 (skill, minRating)
    @Override
    public Page<Developer> searchDevelopers(String skill, Double minRating, Pageable pageable) {
        QDeveloper qDeveloper = QDeveloper.developer;
        QUser qUser = QUser.user;

        // 동적 조건 생성 (null이면 조건 무시)
        // 조건이 있을 때만 동적으로 where절에 추가하는 QueryDSL 도구
        BooleanBuilder builder = new BooleanBuilder();

        if (skill != null && !skill.isBlank()) {
            builder.and(qDeveloper.skills.contains(skill));   // (조건1) skills 목록에 해당 skill 포함 여부
        }
        if (minRating != null) {
            builder.and(qDeveloper.rating.goe(minRating));    // (조건2) rating >= minRating
        }

        // 실제 데이터 조회
        List<Developer> content = queryFactory
                .selectFrom(qDeveloper)
                .join(qDeveloper.user, qUser).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long total = queryFactory
                .select(qDeveloper.count())
                .from(qDeveloper)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}

