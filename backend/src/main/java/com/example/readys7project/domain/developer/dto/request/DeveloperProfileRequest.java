package com.example.readys7project.domain.developer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperProfileRequest {

    @NotBlank(message = "직함은 필수입니다")
    private String title;

    private List<String> skills;
    private String hourlyRate;
    private String responseTime;
    private List<String> portfolio;
}
