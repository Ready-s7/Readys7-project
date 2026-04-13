package com.example.readys7project.domain.user.developer.repository;

import com.example.readys7project.domain.user.developer.entity.Developer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DeveloperQueryRepository {

    // 전체 개발자 목록 조회
    Page<Developer> findAllWithUser(Pageable pageable);

    // 개발자 검색 (skill, minRating)
    Page<Developer> searchDevelopers(String skill, Double minRating, Pageable pageable);
    }
