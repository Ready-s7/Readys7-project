package com.example.readys7project.domain.proposal.service;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.request.UpdateProposalRequestDto;
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
import com.example.readys7project.global.lock.service.LockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProposalServiceTest {

    @InjectMocks
    private ProposalService proposalService;

    @Mock private ProposalRepository proposalRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private DeveloperRepository developerRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProposalTransactionalService proposalTransactionalService;
    @Mock private LockService lockService;

    @Test
    @DisplayName("성공: LockService를 통해 제안서 생성 로직이 호출된다")
    void createProposal_Success() {
        ProposalRequestDto request = new ProposalRequestDto(1L, "Cover Letter", "1000", "1 month");
        String email = "dev@test.com";
        ProposalResponseDto expectedResponse = ProposalResponseDto.builder().id(1L).build();

        given(lockService.executeWithLock(eq("project:1"), any(Supplier.class)))
                .willAnswer(invocation -> ((Supplier<ProposalResponseDto>) invocation.getArgument(1)).get());
        given(proposalTransactionalService.createProposalInternal(request, email)).willReturn(expectedResponse);

        ProposalResponseDto result = proposalService.createProposal(request, email);
        assertThat(result.id()).isEqualTo(1L);
        verify(lockService).executeWithLock(eq("project:1"), any(Supplier.class));
    }

    @Nested
    @DisplayName("제안서 조회")
    class GetProposals {
        @Test
        @DisplayName("성공: 내 제안서 목록")
        void getMyProposals_Success() {
            String email = "dev@test.com";
            PageRequest pr = PageRequest.of(0, 10);
            User user = User.builder().build();
            ReflectionTestUtils.setField(user, "id", 1L);
            Developer dev = Developer.builder().user(user).build();
            ReflectionTestUtils.setField(dev, "id", 1L);
            Project project = Project.builder().title("P").build();
            ReflectionTestUtils.setField(project, "id", 1L);
            Proposal p = Proposal.builder().project(project).developer(dev).status(ProposalStatus.PENDING).build();
            ReflectionTestUtils.setField(p, "id", 1L);

            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(developerRepository.findByUser(user)).willReturn(Optional.of(dev));
            given(proposalRepository.findByDeveloperId(1L, pr)).willReturn(new PageImpl<>(List.of(p), pr, 1));

            Page<ProposalResponseDto> result = proposalService.getMyProposals(email, pr);
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("성공: 단건 조회")
        void getProposal_Success() {
            Project pj = Project.builder().build();
            ReflectionTestUtils.setField(pj, "id", 1L);
            Developer dv = Developer.builder().user(User.builder().build()).build();
            Proposal p = Proposal.builder().project(pj).developer(dv).status(ProposalStatus.PENDING).build();
            ReflectionTestUtils.setField(p, "id", 1L);

            given(proposalRepository.findById(1L)).willReturn(Optional.of(p));
            ProposalResponseDto res = proposalService.getProposal(1L);
            assertThat(res.id()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("상태 변경")
    class UpdateStatus {
        @Test
        @DisplayName("성공: ACCEPTED 시 프로젝트 IN_PROGRESS 변경")
        void updateProposalStatus_Accepted() {
            Long proposalId = 1L;
            String email = "client@test.com";
            UpdateProposalRequestDto req = new UpdateProposalRequestDto(ProposalStatus.ACCEPTED);

            User user = User.builder().userRole(UserRole.CLIENT).build();
            Project pj = Project.builder().build();
            ReflectionTestUtils.setField(pj, "id", 1L);
            ReflectionTestUtils.setField(pj, "status", ProjectStatus.OPEN);

            Proposal p = Proposal.builder().project(pj).developer(Developer.builder().user(User.builder().build()).build()).status(ProposalStatus.PENDING).build();

            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(p));
            given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
            given(proposalRepository.existsByProjectIdAndStatus(1L, ProposalStatus.ACCEPTED)).willReturn(false);
            given(projectRepository.findById(1L)).willReturn(Optional.of(pj));

            proposalService.updateProposalStatus(proposalId, req, email);
            assertThat(pj.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("실패: 중복 수락 방지")
        void updateProposalStatus_AlreadyAccepted() {
            given(proposalRepository.findById(1L)).willReturn(Optional.of(Proposal.builder().project(Project.builder().build()).build()));
            ReflectionTestUtils.setField(((Proposal)proposalRepository.findById(1L).get()).getProject(), "id", 1L);
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(User.builder().build()));
            given(proposalRepository.existsByProjectIdAndStatus(anyLong(), eq(ProposalStatus.ACCEPTED))).willReturn(true);

            assertThatThrownBy(() -> proposalService.updateProposalStatus(1L, new UpdateProposalRequestDto(ProposalStatus.ACCEPTED), "e"))
                    .isInstanceOf(ProposalException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROPOSAL_ALREADY_ACCEPTED);
        }

        @Test
        @DisplayName("성공: 철회 시 프로젝트 OPEN 복구")
        void updateProposalStatus_Withdraw_RestoresOpen() {
            Project pj = Project.builder().build();
            ReflectionTestUtils.setField(pj, "id", 1L);
            ReflectionTestUtils.setField(pj, "status", ProjectStatus.IN_PROGRESS);
            ReflectionTestUtils.setField(pj, "currentProposalCount", 1);

            Proposal p = Proposal.builder().project(pj).developer(Developer.builder().user(User.builder().build()).build()).status(ProposalStatus.PENDING).build();

            given(proposalRepository.findById(1L)).willReturn(Optional.of(p));
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(User.builder().userRole(UserRole.DEVELOPER).build()));
            given(projectRepository.findById(1L)).willReturn(Optional.of(pj));
            given(proposalRepository.existsByProjectIdAndStatus(1L, ProposalStatus.ACCEPTED)).willReturn(false);

            proposalService.updateProposalStatus(1L, new UpdateProposalRequestDto(ProposalStatus.WITHDRAWN), "e");
            assertThat(pj.getStatus()).isEqualTo(ProjectStatus.OPEN);
        }
    }
}
