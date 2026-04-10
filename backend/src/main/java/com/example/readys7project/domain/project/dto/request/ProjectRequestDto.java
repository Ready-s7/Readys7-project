package com.example.readys7project.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ProjectRequestDto(

    @NotBlank(message = "제목은 필수입니다.")
    String title,

    @NotBlank(message = "설명은 필수입니다.")
    String description,

    @NotBlank(message = "카테고리는 필수입니다.")
    Long categoryId,

    @NotNull(message = "예산은 필수입니다.")
    Integer budget,

    @NotNull(message = "기간은 필수입니다.")
    Integer duration,

    String skills

//    @NotNull(message = "최대 지원자 수는 필수입니다.")
//    Integer maxProposals,
//
//    LocalDateTime recruitDeadline
){ }
