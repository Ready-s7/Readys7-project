package com.example.readys7project.domain.user.auth.dto.request;

import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AdminRegisterRequestDto (

        @NotBlank(message = "역할은 필수입니다.")
        AdminRole adminRole

) {}