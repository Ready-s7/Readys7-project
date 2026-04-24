package com.example.readys7project.domain.category.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.search.dto.response.CategoriesGlobalSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.SearchPageResponseDto;
import com.example.readys7project.domain.user.admin.entity.QAdmin;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

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

        BooleanBuilder builder = new BooleanBuilder();

        // name은 필수 입력이므로 무조건 조건에 추가
        builder.and(category.name.containsIgnoreCase(name));

        // description은 optional이므로 null/blank일 때 조건 무시
        if (description != null && !description.isBlank()) {
            builder.and(category.description.containsIgnoreCase(description));
        }

        return queryFactory
                .selectFrom(category)
                .where(builder)
                .orderBy(category.displayOrder.asc()) // 기존 전체조회와 동일한 정렬 기준
                .fetch();
    }

    // 통합 검색 페이징 구현
    @Override
    public SearchPageResponseDto<CategoriesGlobalSearchResponseDto> categoriesGlobalSearch(String keyword, Pageable pageable) {

        QAdmin qAdmin = QAdmin.admin;

        List<CategoriesGlobalSearchResponseDto> content = queryFactory
                .select(Projections.constructor(CategoriesGlobalSearchResponseDto.class,
                        category.id,
                        qAdmin.id,
                        category.name,
                        category.icon,
                        category.description,
                        category.displayOrder
                ))
                .from(category)
                .leftJoin(category.admin, qAdmin)
                .where(searchCondition(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(category.displayOrder.asc())
                .fetch();

        // 카운트 쿼리 분리
        JPAQuery<Long> countQuery = queryFactory
                .select(category.count())
                .from(category)
                .leftJoin(category.admin, qAdmin)
                .where(searchCondition(keyword));

        Page<CategoriesGlobalSearchResponseDto> page =
                PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

        return SearchPageResponseDto.from(page);

    }

    private Predicate searchCondition(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String trimmedKeyword = keyword.trim();
        BooleanBuilder builder = new BooleanBuilder();

        builder.or(nameLike(trimmedKeyword));
        builder.or(descriptionLike(trimmedKeyword));

        return builder.getValue();
    }

    // 카테고리 이름 검색 조건
    private BooleanExpression nameLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return category.name.containsIgnoreCase(keyword);
    }

    private BooleanExpression descriptionLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return category.description.containsIgnoreCase(keyword);
    }
}
