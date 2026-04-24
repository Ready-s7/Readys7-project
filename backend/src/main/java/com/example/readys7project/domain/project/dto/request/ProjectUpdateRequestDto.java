package com.example.readys7project.domain.project.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record ProjectUpdateRequestDto(

        String title,

        String description,

        Long categoryId,

        Long minBudget,

        Long maxBudget,

        Integer duration,

        List<String> skills,

        Integer maxProposalCount
){ }
