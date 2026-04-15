package com.example.readys7project.domain.skill.repository;

import com.example.readys7project.domain.search.dto.response.SkillPopularSearchResponseDto;
import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SkillQueryRepository {
    Page<Skill> findByNameAndCategory(String name, SkillCategory category, Pageable pageable);
    Page<Skill> findAllWithAdminAndUser(Pageable pageable);

    // 인기 검색 페이징 구현
    Page<SkillPopularSearchResponseDto> skillsPopularSearch(String keyword, Pageable pageable);
}
