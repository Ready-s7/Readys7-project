package com.example.readys7project.domain.portfolio.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record PortfolioUpdateRequestDto(

        String title,

        String description,

        String imageUrl,
        String projectUrl,

        List<String> skills

)
{}
