package com.example.readys7project.domain.portfolio.dto;


import lombok.Builder;


import java.time.LocalDateTime;

@Builder
public record PortfolioDto (

     Long id,
    Long developerId,
     String title,
    String description,
    String imageUrl,
    String projectUrl,
     String skills,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
){}
