package com.example.readys7project.domain.user.auth.dto.request;

public record UpdateUserInformationRequestDto (

        String name,

        String phoneNumber,

        String description
) {}
