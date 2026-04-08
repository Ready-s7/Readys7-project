package com.example.readys7project.domain.project.repository;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByClientId(Long clientId);
    List<Project> findByCategory(String category);
    List<Project> findByStatus(ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:status IS NULL OR p.status = :status) AND " +
           "(:skill IS NULL OR :skill MEMBER OF p.skills)")
    List<Project> searchProjects(
        @Param("category") String category,
        @Param("status") ProjectStatus status,
        @Param("skill") String skill
    );
}
