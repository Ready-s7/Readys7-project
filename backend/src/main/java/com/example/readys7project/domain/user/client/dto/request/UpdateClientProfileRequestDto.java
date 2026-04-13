package com.example.readys7project.domain.user.client.dto.request;

import com.example.readys7project.domain.user.enums.ParticipateType;
import jakarta.validation.constraints.NotBlank;

public record UpdateClientProfileRequestDto(

        @NotBlank(message = "직군은 입력은 필수입니다.")
        String title,

        @NotBlank(message = "사업자 유형은 필수입니다.")
        ParticipateType participateType

) {}
