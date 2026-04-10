package com.example.readys7project.domain.user.auth.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserDto (

    Long id,

    String email,

    String name,

    String role,

    LocalDateTime createdAt
) {}
