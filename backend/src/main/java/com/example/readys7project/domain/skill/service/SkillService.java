package com.example.readys7project.domain.skill.service;

import com.example.readys7project.domain.skill.dto.request.CreateSkillRequestDto;
import com.example.readys7project.domain.skill.dto.response.SkillResponseDto;
import com.example.readys7project.domain.skill.dto.request.UpdateSkillRequestDto;
import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SkillException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final AdminRepository adminRepository;

    @Transactional
    public SkillResponseDto createSkill(CreateSkillRequestDto request, String email) {

        // admin 가져오기 (Aspect에서 검증 완료됨)
        Admin admin = adminRepository.findByUserEmail(email).orElseThrow(
                () -> new SkillException(ErrorCode.ADMIN_NOT_FOUND)
        );

        Skill skill = Skill.builder()
                .admin(admin)
                .name(request.name())
                .skillCategory(request.category())
                .build();

        Skill savedSkill = skillRepository.save(skill);

        return convertToDto(savedSkill);
    }

    @Transactional(readOnly = true)
    public Page<SkillResponseDto> getSkills(Pageable pageable) {
        return skillRepository.findAllWithAdminAndUser(pageable).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<SkillResponseDto> searchSkills(String name, SkillCategory category, Pageable pageable) {
        return skillRepository.findByNameAndCategory(name, category, pageable).map(this::convertToDto);
    }

    @Transactional
    @CacheEvict(value = "globalSearch", allEntries = true)
    public SkillResponseDto updateSkill(Long skillId, UpdateSkillRequestDto request) {

        // request 필드가 둘 다 널이면 에러 처리
        if ((request.name() == null || request.name().isBlank()) && request.category() == null) {
            throw new SkillException(ErrorCode.SKILL_UPDATE_DATA_NULL);
        }

        // skill 가져오기
        Skill skill = skillRepository.findById(skillId).orElseThrow(
                () -> new SkillException(ErrorCode.SKILL_NOT_FOUND)
        );

        // update
        skill.updateSkill(request);

        return convertToDto(skill);
    }

    @Transactional
    @CacheEvict(value = "globalSearch", allEntries = true)
    public void deleteSkill(Long skillId) {

        // skill 존재하는지 검증
        boolean existence = skillRepository.existsById(skillId);
        if (!existence) {
            throw new SkillException(ErrorCode.SKILL_NOT_FOUND);
        }

        // skill 삭제
        skillRepository.deleteById(skillId);
    }

    private SkillResponseDto convertToDto(Skill skill) {
        return SkillResponseDto.builder()
                .id(skill.getId())
                .adminId(skill.getAdmin().getId())
                .adminName(skill.getAdmin().getUser().getName())
                .name(skill.getName())
                .category(skill.getSkillCategory())
                .createdAt(skill.getCreatedAt())
                .build();
    }
}
