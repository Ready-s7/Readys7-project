package com.example.readys7project.domain.user.developer.repository;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.user.developer.entity.Developer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DeveloperQueryRepository {

    // 전체 개발자 목록 조회
    Page<Developer> findAllWithUser(Pageable pageable);

    // 개발자 검색 (skill, minRating)
    Page<Developer> searchDevelopers(List<String> skills, Double minRating, Pageable pageable);

    // 내 프로젝트 목록 조회 (ACCEPTED된 제안서의 프로젝트)
    Page<Project> findMyProjects(Developer developer, Pageable pageable);
}
