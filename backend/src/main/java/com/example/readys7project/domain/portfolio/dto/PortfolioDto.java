package com.example.readys7project.domain.portfolio.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioDto {

    private Long id;
    private Long developerId;
    private String title;
    private String description;
    private String imageUrl;
    private String projectUrl;
    private String skills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
