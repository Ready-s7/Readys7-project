package com.example.readys7project.domain.project.repository;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>, ProjectQueryRepository {
    List<Project> findByClientId(Long clientId);
    List<Project> findByCategory(Category category);
    List<Project> findByStatus(ProjectStatus status);
    // 내 프로젝트 목록 조회 페이징
    Page<Project> findByClientId(Long clientId, Pageable pageable);
}
