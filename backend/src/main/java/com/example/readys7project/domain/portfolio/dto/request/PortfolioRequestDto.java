package com.example.readys7project.domain.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;


@Builder
public record
PortfolioRequestDto(
        @NotBlank(message = "포트폴리오 제목은 필수입니다.")
        String title,

        @NotBlank(message = "포트폴리오 설명은 필수입니다.")
        String description,

        String imageUrl,
        String projectUrl,

        @NotEmpty(message = "기술은 한 개 이상 입력해야합니다.")
        List<String> skills
)
{ }
