package com.example.readys7project.domain.project.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ProjectDto(
    Long id,
    Long clientId,
    String title,
    String description,
    String category,
    Long minBudget,
    Long maxBudget,
    Integer duration,
    List<String> skills,
    String status,
    Integer currentProposalCount,
    Integer maxProposalCount,
    String clientName,
    Double clientRating,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
){}
