package com.example.readys7project.domain.user.client.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GetClientProjectsListResponseDto(

        List<ClientProjectSummaryResponseDto> projects,

        int currentPage,

        int size,

        long totalCount,

        int totalPage

) {}
