package com.example.readys7project.domain.user.auth.dto.response;

import com.example.readys7project.domain.user.admin.enums.AdminRole;
import lombok.Builder;

@Builder
public record AdminRegisterResponseDto(

        Long id,

        String email,

        String name,

        Long adminId,

        AdminRole adminRole
) {}
