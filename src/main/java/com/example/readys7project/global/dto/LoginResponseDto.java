package com.example.readys7project.global.dto;

import lombok.Builder;

@Builder
public record LoginResponseDto(
        String refreshToken,
        String email
) {}