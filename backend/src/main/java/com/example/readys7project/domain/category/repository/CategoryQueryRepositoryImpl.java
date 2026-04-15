package com.example.readys7project.domain.category.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.entity.QCategory;
import com.example.readys7project.domain.search.dto.response.CategoryPopularSearchResponseDto;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

    @Override
    public Page<CategoryPopularSearchResponseDto> categoriesPopularSearch(String keyword, Pageable pageable) {

        List<CategoryPopularSearchResponseDto> content = queryFactory
                .select(Projections.constructor(CategoryPopularSearchResponseDto.class,
                        category.id,
                        category.name,
                        category.icon
                ))
                .from(category)
                .where(nameLike(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(category.displayOrder.asc())
                .fetch();

        Long total = queryFactory
                .select(category.count())
                .from(category)
                .where(nameLike(keyword))
                .fetchOne();

        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    private BooleanExpression nameLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return category.name.containsIgnoreCase(keyword);
    }
}
