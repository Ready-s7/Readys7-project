package com.example.readys7project.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Builder
public record ProjectRequestDto(

    @NotBlank(message = "제목은 필수입니다.")
    String title,

    @NotBlank(message = "설명은 필수입니다.")
    String description,

    @NotNull(message = "카테고리는 필수입니다.")
    Long categoryId,

    @NotNull(message = "최저 예산은 필수입니다.")
    Long minBudget,

    @NotNull(message = "최대 예산은 필수입니다.")
    Long maxBudget,

    @NotNull(message = "기간은 필수입니다.")
    Integer duration,

    @NotEmpty(message = "기술은 필수입니다.")
    List<String> skills,

    @NotNull(message = "최대 지원자 수는 필수입니다.")
    Integer maxProposalCount
){ }