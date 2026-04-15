package com.example.readys7project.domain.user.auth.dto.response;

import com.example.readys7project.domain.user.auth.enums.UserRole;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GetUserInformationResponseDto (

        Long id,

        String email,

        UserRole userRole,

        String description,

        LocalDateTime createdAt
) {}
