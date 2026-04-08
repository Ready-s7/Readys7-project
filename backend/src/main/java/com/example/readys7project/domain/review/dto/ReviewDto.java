package com.example.readys7project.domain.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Long id;
    private Long developerId;
    private String developerName;
    private Long clientId;
    private String clientName;
    private Long projectId;
    private String projectTitle;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
