package com.example.readys7project.domain.portfolio.dto;


import lombok.Builder;


import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PortfolioDto (

        Long id,
        Long developerId,
        String title,
        String description,
        String imageUrl,
        String projectUrl,
        List<String> skills,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
){}
