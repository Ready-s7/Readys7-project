package com.example.readys7project.domain.user.admin.dto.response;

import java.util.List;

public record GetAllAdminListResponseDto(

        List<AdminSummaryResponseDto> admins,

        int pageNumber,

        int size,

        long totalElements,

        int totalPages

) {}
