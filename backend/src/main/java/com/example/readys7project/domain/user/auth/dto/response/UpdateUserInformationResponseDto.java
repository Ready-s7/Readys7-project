package com.example.readys7project.domain.user.auth.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UpdateUserInformationResponseDto (

        Long id,

        String email,

        String name,

        String phoneNumber,

        String description,

        LocalDateTime updatedAt
) {}
