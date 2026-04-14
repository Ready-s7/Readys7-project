
package com.example.readys7project.domain.proposal.service;

import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.project.service.ProjectService;
import com.example.readys7project.domain.proposal.dto.ProposalDto;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.request.UpdateProposalRequestDto;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProposalException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    @Transactional
    public ProposalDto createProposal(ProposalRequestDto request, String userEmail) {

        // user 가져오기
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProposalException(ErrorCode.USER_NOT_FOUND));

        // 개발자인지 검증
        if (user.getUserRole() != UserRole.DEVELOPER) {
            throw new ProposalException(ErrorCode.USER_FORBIDDEN);
        }

        // developer 가져오기
        Developer developer = developerRepository.findByUser(user)
                .orElseThrow(() -> new ProposalException(ErrorCode.DEVELOPER_NOT_FOUND));

        // project 가져오기
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new ProposalException(ErrorCode.PROJECT_NOT_FOUND));

        // 이미 제안서를 제출했는지 확인
        proposalRepository.findByProjectIdAndDeveloperId(project.getId(), developer.getId())
                .ifPresent(p -> {
                    throw new ProposalException(ErrorCode.PROPOSAL_ALREADY_EXISTS);
                });

        // proposal 객체 생성
        Proposal proposal = Proposal.builder()
                .project(project)
                .developer(developer)
                .coverLetter(request.coverLetter())
                .proposedBudget(request.proposedBudget())
                .proposedDuration(request.proposedDuration())
                .status(ProposalStatus.PENDING)
                .build();

        // proposal 객체 저장
        Proposal savedProposal = proposalRepository.save(proposal);

        // 프로젝트의 제안 수 증가
        projectService.incrementProposalCount(project.getId());

        return convertToDto(savedProposal);
    }

    @Transactional(readOnly = true)
    public Page<ProposalDto> getProposalsByProject(Long projectId, String email, Pageable pageable) {

        // 프로젝트 가져오기
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new ProposalException(ErrorCode.PROJECT_NOT_FOUND)
        );

        // 현재 사용자 정보 가져오기
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ProposalException(ErrorCode.USER_NOT_FOUND)
        );

        // 현재 사용자가 해당 프로젝트의 제안서 목록을 열람할 권리가 있는 지 검증
        // (본인의 프로젝트이거나 관리자)가 아니면 에러 처리
        if (!(project.getClient().getUser().getId().equals(user.getId()) || user.getUserRole().equals(UserRole.ADMIN))) {
            throw new ProposalException(ErrorCode.USER_FORBIDDEN);
        }

        // 프로젝트 아이디 기준으로 페이지 가져오기
        return proposalRepository.findByProjectId(projectId, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public ProposalDto getProposal(Long proposalId, String email) {

        // proposal 가져오기
        Proposal proposal = proposalRepository.findById(proposalId).orElseThrow(
                () -> new ProposalException(ErrorCode.PROPOSAL_NOT_FOUND)
        );

        // 현재 사용자 정보 가져오기
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new ProposalException(ErrorCode.USER_NOT_FOUND)
        );

        // project 가져오기
        Project project = projectRepository.findById(proposal.getProject().getId()).orElseThrow(
                () -> new ProposalException(ErrorCode.PROJECT_NOT_FOUND)
        );

        // 현재 사용자가 해당 제안서를 조회할 수 있는 지 검증
        // (제안서를 작성한 유저이거나 프로젝트를 작성한 유저이거나 관리자)가 아니면 에러 처리
        if (!(proposal.getDeveloper().getUser().getId().equals(user.getId())
                || project.getClient().getUser().getId().equals(user.getId())
                || user.getUserRole().equals(UserRole.ADMIN)
        )) {
            throw new ProposalException(ErrorCode.USER_FORBIDDEN);
        }

        // 해당 제안서 반환
        return convertToDto(proposal);
    }

    @Transactional(readOnly = true)
    public Page<ProposalDto> getMyProposals(String email, Pageable pageable) {

        // 유저 가져오기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ProposalException(ErrorCode.USER_NOT_FOUND));

        // 개발자인지 확인
        Developer developer = developerRepository.findByUser(user)
                .orElseThrow(() -> new ProposalException(ErrorCode.DEVELOPER_NOT_FOUND));

        // 해당 개발자의 제안서 목록 조회
        return proposalRepository.findByDeveloperId(developer.getId(), pageable)
                .map(this::convertToDto);
    }

    @Transactional
    public ProposalDto updateProposalStatus(Long proposalId, UpdateProposalRequestDto request, String email) {

        // proposal 가져오기
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ProposalException(ErrorCode.PROPOSAL_NOT_FOUND));

        // user 가져오기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ProposalException(ErrorCode.USER_NOT_FOUND));

        // '제안서를 작성한 개발자 본인'이거나
        if (!(proposal.getDeveloper().getUser().getId().equals(user.getId())
                    // '프로젝트를 생성한 클라이언트'이거나
                || proposal.getProject().getClient().getUser().getId().equals(user.getId())
                    // '관리자'가 아니라면 예외 처리
                || user.getUserRole().equals(UserRole.ADMIN)
        )) {
            throw new ProposalException(ErrorCode.USER_FORBIDDEN);
        }

        // ACCEPTED로 변경하려는 경우에만 검증
        if (request.status().equals(ProposalStatus.ACCEPTED)) {
            boolean alreadyAccepted = proposalRepository.existsByProjectIdAndStatus(
                    proposal.getProject().getId(),
                    ProposalStatus.ACCEPTED
            );
            if (alreadyAccepted) {
                throw new ProposalException(ErrorCode.PROPOSAL_ALREADY_ACCEPTED);
            }
        }

        Project project = projectRepository.findById(proposal.getProject().getId()).orElseThrow(
                () -> new ProposalException(ErrorCode.PROJECT_NOT_FOUND)
        );

        try {
            // 상태 변경
            proposal.updateStatus(request.status(), user.getUserRole());

            // 제안서가 승인된다면 프로젝트 상태도 작업 중으로 변경
            if (request.status().equals(ProposalStatus.ACCEPTED)) {
                project.changeStatus(ProjectStatus.IN_PROGRESS);
            }

        } catch (ProposalException e) {
            throw new ProposalException(ErrorCode.PROPOSAL_ACCEPTED_FAILED);
        }

        return convertToDto(proposal);
    }

    private ProposalDto convertToDto(Proposal proposal) {
        return ProposalDto.builder()
                .id(proposal.getId())
                .projectId(proposal.getProject().getId())
                .projectTitle(proposal.getProject().getTitle())
                .developerId(proposal.getDeveloper().getId())
                .developerName(proposal.getDeveloper().getUser().getName())
                .coverLetter(proposal.getCoverLetter())
                .proposedBudget(proposal.getProposedBudget())
                .proposedDuration(proposal.getProposedDuration())
                .status(proposal.getStatus().name().toLowerCase())
                .build();
    }
}
