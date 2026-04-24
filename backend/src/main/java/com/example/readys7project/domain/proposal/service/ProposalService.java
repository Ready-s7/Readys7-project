package com.example.readys7project.domain.proposal.service;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.dto.response.ProposalResponseDto;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.request.UpdateProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.response.ProposalResponseDto;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProjectException;
import com.example.readys7project.global.exception.domain.ProposalException;
import com.example.readys7project.global.lock.service.LockService;
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
    private final ProposalTransactionalService proposalTransactionalService;
    private final LockService lockService;

    public ProposalResponseDto createProposal(ProposalRequestDto request, String userEmail) {
        return lockService.executeWithLock(
                "project:" + request.projectId(), // Lock Key
                () -> proposalTransactionalService.createProposalInternal(request, userEmail)
        );
    }

    @Transactional(readOnly = true)
    public Page<ProposalResponseDto> getProposalsByProject(Long projectId, Pageable pageable) {
        return proposalRepository.findByProjectId(projectId, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public ProposalResponseDto getProposal(Long proposalId) {
        Proposal proposal = findProposal(proposalId);
        return convertToDto(proposal);
    }

    @Transactional(readOnly = true)
    public Page<ProposalResponseDto> getMyProposals(String email, Pageable pageable) {
        User user = findUser(email);
        Developer developer = findDeveloper(user);

        return proposalRepository.findByDeveloperId(developer.getId(), pageable)
                .map(this::convertToDto);
    }

    @Transactional
    public ProposalResponseDto updateProposalStatus(Long proposalId, UpdateProposalRequestDto request, String email) {
        Proposal proposal = findProposal(proposalId);
        User user = findUser(email);

        if (request.status().equals(ProposalStatus.ACCEPTED)) {
            boolean alreadyAccepted = proposalRepository.existsByProjectIdAndStatus(
                    proposal.getProject().getId(),
                    ProposalStatus.ACCEPTED
            );
            if (alreadyAccepted) {
                throw new ProposalException(ErrorCode.PROPOSAL_ALREADY_ACCEPTED);
            }
        }

        Project project = findProject(proposal.getProject().getId());

        try {
            proposal.updateStatus(request.status(), user.getUserRole());

            if (request.status().equals(ProposalStatus.ACCEPTED)) {
                project.changeStatus(ProjectStatus.IN_PROGRESS);
            }

            if (request.status().equals(ProposalStatus.WITHDRAWN) || request.status().equals(ProposalStatus.REJECTED)) {
                project.decreaseProposalCount();
                boolean existence = proposalRepository.existsByProjectIdAndStatus(project.getId(), ProposalStatus.ACCEPTED);
                if (!existence) {
                    project.changeStatus(ProjectStatus.OPEN);
                }
            }

        } catch (ProposalException e) {
            throw new ProposalException(ErrorCode.PROPOSAL_ACCEPTED_FAILED);
        }

        return convertToDto(proposal);
    }

    private ProposalResponseDto convertToDto(Proposal proposal) {
        return ProposalResponseDto.builder()
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
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }

    private User findUser(String userEmail) {
        return userRepository.findByEmail(userEmail).orElseThrow(
                () -> new ProposalException(ErrorCode.USER_NOT_FOUND)
        );
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(
                () -> new ProjectException(ErrorCode.PROJECT_NOT_FOUND)
        );
    }

    private Developer findDeveloper(User user) {
        return developerRepository.findByUser(user).orElseThrow(
                () -> new ProposalException(ErrorCode.DEVELOPER_NOT_FOUND)
        );
    }

    private Proposal findProposal(Long proposalId) {
        return proposalRepository.findById(proposalId).orElseThrow(
                () -> new ProposalException(ErrorCode.PROPOSAL_NOT_FOUND)
        );
    }
}
