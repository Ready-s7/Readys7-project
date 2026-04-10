package com.example.readys7project.domain.project.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ProjectDto(
    Long id,
    String title,
    String description,
    String category,
    Integer budget,
    Integer duration,
    String skills,
    String status,
//    Integer currentProposals,
//    Integer maxProposals,
    String clientName,
    Double clientRating,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
){}
