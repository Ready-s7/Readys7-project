package com.example.readys7project.domain.proposal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposalDto {
    private Long id;
    private Long projectId;
    private String projectTitle;
    private Long developerId;
    private String developerName;
    private String coverLetter;
    private String proposedBudget;
    private String proposedDuration;
    private String status;
    private LocalDateTime submittedAt;
}
