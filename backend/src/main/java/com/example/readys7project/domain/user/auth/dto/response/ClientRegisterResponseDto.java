package com.example.readys7project.domain.user.auth.dto.response;

import com.example.readys7project.domain.user.auth.dto.UserDto;
import lombok.Builder;

@Builder
public record ClientRegisterResponseDto (

        UserDto user,

        Long clientId,

        String title
) {}
