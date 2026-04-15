package com.example.readys7project.domain.user.client.dto.response;

import com.example.readys7project.domain.project.enums.ProjectStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ClientProjectsListResponseDto(
        Long id,
        String title,
        String description,
        String category,
        Long minBudget,
        Long maxBudget,
        Integer duration,
        ProjectStatus status,
        Integer currentProposalCount,
        Integer maxProposalCount,
        List<String> skills,
        LocalDateTime createdAt
) {}
