package com.example.readys7project.domain.category.repository;

import com.example.readys7project.domain.category.entity.Category;
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
}
