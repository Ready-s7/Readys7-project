package com.example.readys7project.domain.category.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.entity.QCategory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.example.readys7project.domain.category.entity.QCategory.category;
import static com.example.readys7project.domain.user.admin.entity.QAdmin.admin;

@RequiredArgsConstructor
public class CategoryQueryRepositoryImpl implements CategoryQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Category> findAllWithAdminOrderByDisplayOrderAsc() {
        return queryFactory
                .selectFrom(category)
                .join(category.admin, admin).fetchJoin()
                .orderBy(category.displayOrder.asc())
                .fetch();
    }

    @Override
    public List<Category> findByNameContainingWithAdmin(String name) {
        return queryFactory
                .selectFrom(category)
                .join(category.admin, admin).fetchJoin()
                .where(category.name.contains(name))
                .orderBy(category.displayOrder.asc())
                .fetch();
    }

    @Override
    public List<Category> searchCategories(String name, String description) {

        QCategory qCategory = QCategory.category;
        BooleanBuilder builder = new BooleanBuilder();

        // name은 필수 입력이므로 무조건 조건에 추가
        builder.and(qCategory.name.containsIgnoreCase(name));

        // description은 optional이므로 null/blank일 때 조건 무시
        if (description != null && !description.isBlank()) {
            builder.and(qCategory.description.containsIgnoreCase(description));
        }

        return queryFactory
                .selectFrom(qCategory)
                .where(builder)
                .orderBy(qCategory.displayOrder.asc()) // 기존 전체조회와 동일한 정렬 기준
                .fetch();
    }
}
