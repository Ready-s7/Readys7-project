package com.example.readys7project.domain.user.admin.dto.response;

import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;

import java.time.LocalDateTime;

public record AdminSummaryResponseDto(

        Long id,

        String email,

        String name,

        AdminRole adminRole,

        AdminStatus status,

        LocalDateTime createdAt
) {
    public AdminSummaryResponseDto(Admin admin) {
        this(
                admin.getUser().getId(),
                admin.getUser().getEmail(),
                admin.getUser().getName(),
                admin.getAdminRole(),
                admin.getStatus(),
                admin.getUser().getCreatedAt()

        );
    }
}
