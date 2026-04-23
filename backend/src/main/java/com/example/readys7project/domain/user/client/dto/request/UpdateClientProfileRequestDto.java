package com.example.readys7project.domain.user.client.dto.request;

import com.example.readys7project.domain.user.enums.ParticipateType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateClientProfileRequestDto(

        @NotBlank(message = "직군 입력은 필수입니다.")
        @Size(max = 50, message = "직군은 50자 이내여야 합니다.")
        String title,

        @NotNull(message = "사업자 유형은 필수입니다.")
        ParticipateType participateType

) {}
