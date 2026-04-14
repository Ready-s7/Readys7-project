package com.example.readys7project.domain.skill.service;

import com.example.readys7project.domain.skill.dto.request.CreateSkillRequestDto;
import com.example.readys7project.domain.skill.dto.SkillDto;
import com.example.readys7project.domain.skill.dto.request.UpdateSkillRequestDto;
import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SkillException;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Transactional
    public SkillDto createSkill(CreateSkillRequestDto request, String email) {

        // user 가져오기
        User user = findUser(email);

        // 관리자인지 검증
        validateAdmin(user);

        // admin 가져오기
        Admin admin = findAdmin(user);

        // 해당 관리자가 승인 상태인지 검증
        validateAdminStatus(admin);

        Skill skill = Skill.builder()
                .admin(admin)
                .name(request.name())
                .skillCategory(request.category())
                .build();

        Skill savedSkill = skillRepository.save(skill);

        return convertToDto(savedSkill);
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> getSkills(Pageable pageable) {
        return skillRepository.findAllWithAdminAndUser(pageable).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Page<SkillDto> searchSkills(String name, SkillCategory category, Pageable pageable) {
        return skillRepository.findByNameAndCategory(name, category, pageable).map(this::convertToDto);
    }

    @Transactional
    public SkillDto updateSkill(Long skillId, UpdateSkillRequestDto request, String email) {

        // request 필드가 둘 다 널이면 에러 처리
        if ((request.name() == null || request.name().isBlank()) && request.category() == null) {
            throw new SkillException(ErrorCode.SKILL_UPDATE_DATA_NULL);
        }

        // user 가져오기
        User user = findUser(email);

        // 관리자인지 검증
        validateAdmin(user);

        // admin 가져오기
        Admin admin = findAdmin(user);

        // 해당 관리자가 승인 상태인지 검증
        validateAdminStatus(admin);

        // skill 가져오기
        Skill skill = skillRepository.findById(skillId).orElseThrow(
                () -> new SkillException(ErrorCode.SKILL_NOT_FOUND)
        );

        // update
        skill.updateSkill(request);

        return convertToDto(skill);
    }

    @Transactional
    public void deleteSkill(Long skillId, String email) {

        // user 가져오기
        User user = findUser(email);

        // 관리자인지 검증
        validateAdmin(user);

        // admin 가져오기
        Admin admin = findAdmin(user);

        // 해당 관리자가 승인 상태인지 검증
        validateAdminStatus(admin);

        // skill 존재하는지 검증
        boolean existence = skillRepository.existsById(skillId);
        if (!existence) {
            throw new SkillException(ErrorCode.SKILL_NOT_FOUND);
        }

        // skill 삭제
        skillRepository.deleteById(skillId);
    }

    private SkillDto convertToDto(Skill skill) {
        return SkillDto.builder()
                .id(skill.getId())
                .adminId(skill.getAdmin().getId())
                .adminName(skill.getAdmin().getUser().getName())
                .name(skill.getName())
                .category(skill.getSkillCategory())
                .createdAt(skill.getCreatedAt())
                .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new SkillException(ErrorCode.USER_NOT_FOUND)
        );
    }

    private void validateAdmin(User user) {
        if (!user.getUserRole().equals(UserRole.ADMIN)) {
            throw new SkillException(ErrorCode.USER_FORBIDDEN);
        }
    }

    private Admin findAdmin(User user) {
        return adminRepository.findByUser(user).orElseThrow(
                () -> new SkillException(ErrorCode.ADMIN_NOT_FOUND)
        );
    }

    private void validateAdminStatus(Admin admin) {
        if (!admin.getStatus().equals(AdminStatus.APPROVED)) {
            throw new SkillException(ErrorCode.ADMIN_NOT_APPROVED);
        }
    }
}
