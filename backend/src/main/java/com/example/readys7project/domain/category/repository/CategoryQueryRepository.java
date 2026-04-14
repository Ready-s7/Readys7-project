package com.example.readys7project.domain.category.repository;

import com.example.readys7project.domain.category.entity.Category;

import java.util.List;

public interface CategoryQueryRepository {

    List<Category> findAllWithAdminOrderByDisplayOrderAsc();

    List<Category> findByNameContainingWithAdmin(String name);

    List<Category> searchCategories(String name, String description);
}
