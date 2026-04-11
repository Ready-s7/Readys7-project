package com.example.readys7project.domain.user.admin.dto.response;

import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;

import java.time.LocalDateTime;

public record UpdateAdminStatusResponseDto (

        Long id,

        String name,

        AdminRole adminRole,

        AdminStatus adminStatus,

        LocalDateTime updatedAt
) {}
