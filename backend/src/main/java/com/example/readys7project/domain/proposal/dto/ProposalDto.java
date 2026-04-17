package com.example.readys7project.domain.proposal.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ProposalDto(
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
