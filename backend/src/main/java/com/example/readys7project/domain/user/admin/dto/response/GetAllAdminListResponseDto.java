package com.example.readys7project.domain.user.admin.dto.response;

import java.util.List;

public record GetAllAdminListResponseDto(

        List<AdminSummaryResponseDto> admins,

        int currentPage,

        int size,

        long totalCount,

        int totalPage

) {}
