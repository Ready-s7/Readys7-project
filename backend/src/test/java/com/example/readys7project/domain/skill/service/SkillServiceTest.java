package com.example.readys7project.domain.skill.service;

import com.example.readys7project.domain.skill.dto.request.CreateSkillRequestDto;
import com.example.readys7project.domain.skill.dto.request.UpdateSkillRequestDto;
import com.example.readys7project.domain.skill.dto.response.SkillResponseDto;
import com.example.readys7project.domain.skill.entity.Skill;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SkillException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @InjectMocks
    private SkillService skillService;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private AdminRepository adminRepository;

    private User createTestUser() {
        User user = User.builder()
                .email("admin@test.com")
                .name("관리자")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Admin createTestAdmin(User user) {
        Admin admin = Admin.builder()
                .user(user)
                .build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        return admin;
    }

    private Skill createTestSkill(Admin admin, String name, SkillCategory category) {
        Skill skill = Skill.builder()
                .admin(admin)
                .name(name)
                .skillCategory(category)
                .build();
        ReflectionTestUtils.setField(skill, "id", 1L);
        return skill;
    }

    @Nested
    @DisplayName("기술 생성 테스트")
    class CreateSkill {
        @Test
        @DisplayName("성공: 기술을 생성하고 DTO를 반환한다")
        void createSkill_Success() {
            // given
            String email = "admin@test.com";
            CreateSkillRequestDto request = new CreateSkillRequestDto("Java", SkillCategory.BACKEND);
            User user = createTestUser();
            Admin admin = createTestAdmin(user);
            Skill skill = createTestSkill(admin, "Java", SkillCategory.BACKEND);

            given(adminRepository.findByUserEmail(email)).willReturn(Optional.of(admin));
            given(skillRepository.save(any(Skill.class))).willReturn(skill);

            // when
            SkillResponseDto response = skillService.createSkill(request, email);

            // then
            assertThat(response.name()).isEqualTo(request.name());
            assertThat(response.category()).isEqualTo(request.category());
            verify(skillRepository, times(1)).save(any(Skill.class));
        }

        @Test
        @DisplayName("실패: 관리자를 찾을 수 없는 경우 예외가 발생한다")
        void createSkill_Fail_AdminNotFound() {
            // given
            String email = "notfound@test.com";
            CreateSkillRequestDto request = new CreateSkillRequestDto("Java", SkillCategory.BACKEND);

            given(adminRepository.findByUserEmail(email)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> skillService.createSkill(request, email))
                    .isInstanceOf(SkillException.class)
                    .hasMessage(ErrorCode.ADMIN_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("기술 조회 테스트")
    class GetSkills {
        @Test
        @DisplayName("성공: 기술 목록을 페이징하여 조회한다")
        void getSkills_Success() {
            // given
            PageRequest pageRequest = PageRequest.of(0, 10);
            User user = createTestUser();
            Admin admin = createTestAdmin(user);
            Skill skill = createTestSkill(admin, "Java", SkillCategory.BACKEND);
            Page<Skill> skillPage = new PageImpl<>(List.of(skill), pageRequest, 1);

            given(skillRepository.findAllWithAdminAndUser(pageRequest)).willReturn(skillPage);

            // when
            Page<SkillResponseDto> result = skillService.getSkills(pageRequest);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Java");
        }
    }

    @Nested
    @DisplayName("기술 검색 테스트")
    class SearchSkills {
        @Test
        @DisplayName("성공: 이름과 카테고리로 기술을 검색한다")
        void searchSkills_Success() {
            // given
            String name = "Java";
            SkillCategory category = SkillCategory.BACKEND;
            PageRequest pageRequest = PageRequest.of(0, 10);
            User user = createTestUser();
            Admin admin = createTestAdmin(user);
            Skill skill = createTestSkill(admin, "Java", SkillCategory.BACKEND);
            Page<Skill> skillPage = new PageImpl<>(List.of(skill), pageRequest, 1);

            given(skillRepository.findByNameAndCategory(name, category, pageRequest)).willReturn(skillPage);

            // when
            Page<SkillResponseDto> result = skillService.searchSkills(name, category, pageRequest);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo(name);
        }
    }

    @Nested
    @DisplayName("기술 수정 테스트")
    class UpdateSkill {
        @Test
        @DisplayName("성공: 기술 정보를 수정한다")
        void updateSkill_Success() {
            // given
            Long skillId = 1L;
            UpdateSkillRequestDto request = new UpdateSkillRequestDto("Python", SkillCategory.BACKEND);
            User user = createTestUser();
            Admin admin = createTestAdmin(user);
            Skill skill = createTestSkill(admin, "Java", SkillCategory.BACKEND);

            given(skillRepository.findById(skillId)).willReturn(Optional.of(skill));

            // when
            SkillResponseDto response = skillService.updateSkill(skillId, request);

            // then
            assertThat(response.name()).isEqualTo("Python");
        }

        @Test
        @DisplayName("실패: 수정 데이터가 모두 null인 경우 예외가 발생한다")
        void updateSkill_Fail_DataNull() {
            // given
            Long skillId = 1L;
            UpdateSkillRequestDto request = new UpdateSkillRequestDto(null, null);

            // when & then
            assertThatThrownBy(() -> skillService.updateSkill(skillId, request))
                    .isInstanceOf(SkillException.class)
                    .hasMessage(ErrorCode.SKILL_UPDATE_DATA_NULL.getMessage());
        }

        @Test
        @DisplayName("실패: 기술을 찾을 수 없는 경우 예외가 발생한다")
        void updateSkill_Fail_NotFound() {
            // given
            Long skillId = 99L;
            UpdateSkillRequestDto request = new UpdateSkillRequestDto("Python", SkillCategory.BACKEND);

            given(skillRepository.findById(skillId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> skillService.updateSkill(skillId, request))
                    .isInstanceOf(SkillException.class)
                    .hasMessage(ErrorCode.SKILL_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("기술 삭제 테스트")
    class DeleteSkill {
        @Test
        @DisplayName("성공: 기술을 삭제한다")
        void deleteSkill_Success() {
            // given
            Long skillId = 1L;
            given(skillRepository.existsById(skillId)).willReturn(true);

            // when
            skillService.deleteSkill(skillId);

            // then
            verify(skillRepository, times(1)).deleteById(skillId);
        }

        @Test
        @DisplayName("실패: 삭제할 기술을 찾을 수 없는 경우 예외가 발생한다")
        void deleteSkill_Fail_NotFound() {
            // given
            Long skillId = 99L;
            given(skillRepository.existsById(skillId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> skillService.deleteSkill(skillId))
                    .isInstanceOf(SkillException.class)
                    .hasMessage(ErrorCode.SKILL_NOT_FOUND.getMessage());
        }
    }
}
