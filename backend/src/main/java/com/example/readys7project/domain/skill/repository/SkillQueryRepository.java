package com.example.readys7project.domain.skill.repository;

import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SkillQueryRepository {
    Page<Skill> findByNameAndCategory(String name, SkillCategory category, Pageable pageable);
    Page<Skill> findAllWithAdminAndUser(Pageable pageable);
}
