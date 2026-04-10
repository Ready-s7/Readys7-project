
package com.example.readys7project.domain.proposal.service;

import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.project.service.ProjectService;
import com.example.readys7project.domain.proposal.dto.ProposalDto;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequest;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProposalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    @Transactional
    public ProposalDto createProposal(ProposalRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProposalException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != UserRole.DEVELOPER) {
            throw new ProposalException(ErrorCode.USER_FORBIDDEN);
        }

        Developer developer = developerRepository.findByUser(user)
                .orElseThrow(() -> new ProposalException(ErrorCode.DEVELOPER_NOT_FOUND));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ProposalException(ErrorCode.PROJECT_NOT_FOUND));

        // 이미 제안서를 제출했는지 확인
        proposalRepository.findByProjectIdAndDeveloperId(project.getId(), developer.getId())
                .ifPresent(p -> {
                    throw new ProposalException(ErrorCode.PROPOSAL_ALREADY_EXISTS);
                });

        Proposal proposal = Proposal.builder()
                .project(project)
                .developer(developer)
                .coverLetter(request.getCoverLetter())
                .proposedBudget(request.getProposedBudget())
                .proposedDuration(request.getProposedDuration())
                .status(ProposalStatus.PENDING)
                .build();

        proposal = proposalRepository.save(proposal);

        // 프로젝트의 제안 수 증가
        projectService.incrementProposalCount(project.getId());

        return convertToDto(proposal);
    }

    @Transactional(readOnly = true)
    public List<ProposalDto> getProposalsByProject(Long projectId) {
        return proposalRepository.findByProjectId(projectId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProposalDto> getProposalsByDeveloper(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProposalException(ErrorCode.USER_NOT_FOUND));

        Developer developer = developerRepository.findByUser(user)
                .orElseThrow(() -> new ProposalException(ErrorCode.DEVELOPER_NOT_FOUND));

        return proposalRepository.findByDeveloperId(developer.getId()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProposalDto updateProposalStatus(Long proposalId, String status, String userEmail) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ProposalException(ErrorCode.PROPOSAL_NOT_FOUND));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ProposalException(ErrorCode.USER_NOT_FOUND));

        // 클라이언트만 제안서 상태 변경 가능
        if (!proposal.getProject().getClient().getId().equals(user.getId())) {
            throw new ProposalException(ErrorCode.USER_FORBIDDEN);
        }

        proposal.setStatus(ProposalStatus.valueOf(status.toUpperCase()));
        proposal = proposalRepository.save(proposal);

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
