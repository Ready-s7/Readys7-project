package com.example.readys7project.domain.category.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.search.dto.response.CategoriesGlobalSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.SearchPageResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryQueryRepository {

    List<Category> findAllWithAdminOrderByDisplayOrderAsc();

    List<Category> findByNameContainingWithAdmin(String name);

    List<Category> searchCategories(String name, String description);

    // 통합 검색 페이징 구현
    SearchPageResponseDto<CategoriesGlobalSearchResponseDto> categoriesGlobalSearch(String keyword, Pageable pageable);
}
