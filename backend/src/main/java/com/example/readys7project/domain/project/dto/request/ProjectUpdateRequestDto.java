package com.example.readys7project.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@Builder
public record ProjectUpdateRequestDto(

        String title,

        String description,

        Long categoryId,

        Integer minBudget,

        Integer maxBudget,

        Integer duration,

        List<String> skills,

        Integer maxProposalCount
){ }
