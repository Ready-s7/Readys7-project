package com.example.readys7project.domain.developer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperDto {
    private Long id;
    private String name;
    private String title;
    private Double rating;
    private Integer reviewCount;
    private Integer completedProjects;
    private List<String> skills;
    private String hourlyRate;
    private String responseTime;
    private String description;
    private List<String> portfolio;
    private String location;
    private String avatarUrl;
    private Boolean availableForWork;
}
