package com.example.readys7project.domain.user.client.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GetAllClientsListResponseDto (

        List<ClientSummaryResponseDto> clients,

        int currentPage,

        int size,

        long totalCount,

        int totalPage
) {}
