package com.example.readys7project.domain.user.auth.dto.response;

import lombok.Builder;

@Builder
public record DeveloperRegisterResponseDto(

        Long id,

        String email,

        String name,

        Long developerId,

        String title
) {
}
