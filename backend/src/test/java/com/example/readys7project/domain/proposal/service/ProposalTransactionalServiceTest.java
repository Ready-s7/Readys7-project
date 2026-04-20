package com.example.readys7project.domain.proposal.service;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.project.service.ProjectService;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.response.ProposalResponseDto;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProposalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProposalTransactionalServiceTest {

    @InjectMocks
    private ProposalTransactionalService proposalTransactionalService;

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectService projectService;

    @Nested
    @DisplayName("제안서 생성 내부 로직 테스트")
    class CreateProposalInternal {
        @Test
        @DisplayName("성공: 제안서를 생성하고 프로젝트 제안 수를 증가시킨다")
        void createProposalInternal_Success() {
            // given
            String email = "dev@test.com";
            ProposalRequestDto request = new ProposalRequestDto(1L, "Cover Letter", "1000", "1 month");

            User user = User.builder().userRole(UserRole.DEVELOPER).build();
            ReflectionTestUtils.setField(user, "id", 1L);
            Developer developer = Developer.builder().user(user).build();
            ReflectionTestUtils.setField(developer, "id", 1L);

            Project project = Project.builder()
                    .maxProposalCount(5)
                    .title("Project Title")
                    .build();
            ReflectionTestUtils.setField(project, "id", 1L);
            ReflectionTestUtils.setField(project, "currentProposalCount", 0);

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));
            given(proposalRepository.findByProjectIdAndDeveloperId(1L, 1L)).willReturn(Optional.empty());
            given(proposalRepository.save(any(Proposal.class))).willAnswer(inv -> {
                Proposal p = inv.getArgument(0);
                ReflectionTestUtils.setField(p, "id", 100L);
                return p;
            });

            // when
            ProposalResponseDto result = proposalTransactionalService.createProposalInternal(request, email);

            // then
            assertThat(result.id()).isEqualTo(100L);
            verify(projectService).incrementProposalCount(1L);
            verify(proposalRepository).save(any(Proposal.class));
        }

        @Test
        @DisplayName("실패: 제안 슬롯이 가득 찬 경우 예외가 발생한다")
        void createProposalInternal_SlotFull() {
            // given
            String email = "dev@test.com";
            ProposalRequestDto request = new ProposalRequestDto(1L, "Cover Letter", "1000", "1 month");

            User user = User.builder().userRole(UserRole.DEVELOPER).build();
            Developer developer = Developer.builder().user(user).build();
            Project project = Project.builder()
                    .maxProposalCount(5)
                    .build();
            ReflectionTestUtils.setField(project, "id", 1L);
            ReflectionTestUtils.setField(project, "currentProposalCount", 5);

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));

            // when & then
            assertThatThrownBy(() -> proposalTransactionalService.createProposalInternal(request, email))
                    .isInstanceOf(ProposalException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROPOSAL_SLOT_FULL);
        }

        @Test
        @DisplayName("실패: 개발자 역할이 아닌 경우 USER_FORBIDDEN 예외가 발생한다")
        void createProposalInternal_UserForbidden() {
            // given
            String email = "client@test.com";
            ProposalRequestDto request = new ProposalRequestDto(1L, "Cover Letter", "1000", "1 month");
            User user = User.builder().userRole(UserRole.CLIENT).build();

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> proposalTransactionalService.createProposalInternal(request, email))
                    .isInstanceOf(ProposalException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_FORBIDDEN);
        }

        @Test
        @DisplayName("실패: 이미 제안서를 제출한 프로젝트인 경우 PROPOSAL_ALREADY_EXISTS 예외가 발생한다")
        void createProposalInternal_AlreadyExists() {
            // given
            String email = "dev@test.com";
            ProposalRequestDto request = new ProposalRequestDto(1L, "Cover Letter", "1000", "1 month");

            User user = User.builder().userRole(UserRole.DEVELOPER).build();
            Developer developer = Developer.builder().user(user).build();
            ReflectionTestUtils.setField(developer, "id", 1L);

            Project project = Project.builder()
                    .maxProposalCount(5)
                    .build();
            ReflectionTestUtils.setField(project, "id", 1L);
            ReflectionTestUtils.setField(project, "currentProposalCount", 0);

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));
            given(proposalRepository.findByProjectIdAndDeveloperId(1L, 1L)).willReturn(Optional.of(Proposal.builder().build()));

            // when & then
            assertThatThrownBy(() -> proposalTransactionalService.createProposalInternal(request, email))
                    .isInstanceOf(ProposalException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROPOSAL_ALREADY_EXISTS);
        }
    }
}
