package com.example.readys7project.domain.user.auth.dto.response;

import java.time.LocalDateTime;

public record UpdateUserInformationResponseDto (

        Long id,

        String email,

        String name,

        String phoneNumber,

        String description,

        LocalDateTime updatedAt
) {}
