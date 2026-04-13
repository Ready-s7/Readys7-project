package com.example.readys7project.domain.category.repository;

import com.example.readys7project.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // 카테고리 이름 중복 체크용
    boolean existsByName(String name);

    // 카테고리 이름 기준 LIKE 검색용
    List<Category> findByNameContaining(String name);
}
