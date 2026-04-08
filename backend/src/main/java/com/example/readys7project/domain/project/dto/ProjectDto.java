package com.example.readys7project.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProjectDto {
    private Long id;
    private String title;
    private String description;
    private String category;
    private String budget;
    private String duration;
    private List<String> skills;
    private String status;
    private Integer proposalCount;
    private String clientName;
    private Double clientRating;
    private LocalDateTime postedDate;
    private LocalDateTime updatedAt;
}
