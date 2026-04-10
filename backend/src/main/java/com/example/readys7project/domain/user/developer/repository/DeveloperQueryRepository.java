package com.example.readys7project.domain.user.developer.repository;

import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.entity.QDeveloper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeveloperQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Developer> searchDevelopers(String skill, Double minRating, Pageable pageable) {
        QDeveloper developer = QDeveloper.developer;

        // 조건에 맞는 개발자 목록 조회
        List<Developer> content = queryFactory
                .selectFrom(developer)
                .where(
                        skillContains(skill),      // 스킬 조건 (null이면 무시)
                        minRatingGoe(minRating)    // 최소 평점 조건 (null이면 무시)
                )
                .offset(pageable.getOffset())      // 시작 위치
                .limit(pageable.getPageSize())     // 페이지 크기
                .fetch();

        // 전체 개수 조회 (페이징 정보 계산용)
        Long total = queryFactory
                .select(developer.count())
                .from(developer)
                .where(
                        skillContains(skill),
                        minRatingGoe(minRating)
                )
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 스킬 포함 여부 조건 (skill이 null이거나 비어있으면 null 반환 -> 조건 무시)
    private BooleanExpression skillContains(String skill) {
        if (skill == null || skill.isBlank()) {
            return null;
        }
        return QDeveloper.developer.skills.contains(skill);
    }

    // 최소 평점 이상 조건 (minRating이 null이면 null 반환 -> 조건 무시)
    private BooleanExpression minRatingGoe(Double minRating) {
        if (minRating == null) {
            return null;
        }
        return QDeveloper.developer.rating.goe(minRating);
    }
}

