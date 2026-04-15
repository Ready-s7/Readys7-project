package com.example.readys7project.domain.user.auth.dto.response;

import lombok.Builder;

@Builder
public record ClientRegisterResponseDto (

        Long id,

        String email,

        String name,

        Long clientId,

        String title
) {}
