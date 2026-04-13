package com.example.readys7project.domain.user.auth.dto.response;

import com.example.readys7project.domain.user.auth.enums.UserRole;

import java.time.LocalDateTime;

public record GetUserInformationResponseDto (

        Long id,

        String email,

        UserRole userRole,

        String description,

        LocalDateTime createdAt
) {}
