package com.example.readys7project.domain.project.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectQueryRepository {

    /**
     * 프로젝트 검색 (QueryDSL 동적 쿼리)
     * - category, status, skills 조건으로 필터링
     * - 각 조건은 null 또는 empty일 경우 무시 (동적 쿼리)
     */
    Page<Project> searchProjects(
            Category category,
            ProjectStatus status,
            List<String> skills,
            Pageable pageable
    );
}
