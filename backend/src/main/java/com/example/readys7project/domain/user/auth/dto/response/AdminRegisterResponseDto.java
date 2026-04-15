package com.example.readys7project.domain.user.auth.dto.response;

import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.auth.dto.UserDto;
import lombok.Builder;

@Builder
public record AdminRegisterResponseDto(

        UserDto user,

        Long adminId,

        AdminRole adminRole
) {}
