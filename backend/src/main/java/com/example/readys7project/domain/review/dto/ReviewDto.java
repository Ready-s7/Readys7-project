package com.example.readys7project.domain.review.dto;

import com.example.readys7project.domain.review.enums.ReviewRole;
import lombok.Builder;


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
        ReviewRole writerRole,
        Integer rating,
        String comment,
        LocalDateTime createdAt
){}
