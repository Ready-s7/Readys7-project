package com.example.readys7project.domain.proposal.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProposalResponseDto(
        Long id,
        Long projectId,
        String projectTitle,
        Long developerId,
        Long developerUserId,
        String developerName,
        String coverLetter,
        String proposedBudget,
        String proposedDuration,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
