package com.example.readys7project.domain.proposal.service;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.project.service.ProjectService;
import com.example.readys7project.domain.proposal.dto.ProposalDto;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProposalTransactionalService {

    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    @Transactional
    public ProposalDto createProposalInternal(ProposalRequestDto request, String userEmail) {

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

        // 슬롯 검증
        if (project.getCurrentProposalCount() >= project.getMaxProposalCount()) {
            throw new ProposalException(ErrorCode.PROPOSAL_SLOT_FULL);
        }

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

    private ProposalDto convertToDto(Proposal proposal) {
        return ProposalDto.builder()
                .id(proposal.getId())
                .projectId(proposal.getProject().getId())
                .projectTitle(proposal.getProject().getTitle())
                .developerId(proposal.getDeveloper().getId())
                .developerUserId(proposal.getDeveloper().getUser().getId())
                .developerName(proposal.getDeveloper().getUser().getName())
                .coverLetter(proposal.getCoverLetter())
                .proposedBudget(proposal.getProposedBudget())
                .proposedDuration(proposal.getProposedDuration())
                .status(proposal.getStatus().name().toUpperCase())
                .build();
    }
}
