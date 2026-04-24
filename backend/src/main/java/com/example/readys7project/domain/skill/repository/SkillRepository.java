package com.example.readys7project.domain.skill.repository;

import com.example.readys7project.domain.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long>, SkillQueryRepository {

    // IN 절로 한번에 조회
    List<Skill> findAllByNameIn(List<String> skillNames);
}
