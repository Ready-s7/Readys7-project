package com.example.readys7project.domain.category.repository;

import com.example.readys7project.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, CategoryQueryRepository {

    Optional<Category> findByName(String name);

    // 카테고리 이름 중복 체크용
    boolean existsByName(String name);
}
