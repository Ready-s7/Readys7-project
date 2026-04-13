package com.example.readys7project.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Builder
public record ReviewDto (
        Long id,
        Long developerId,
        String developerName,
        Long clientId,
        String clientName,
        Long projectId,
        String projectTitle,
        Integer rating,
        String comment,
        LocalDateTime createdAt
){}
