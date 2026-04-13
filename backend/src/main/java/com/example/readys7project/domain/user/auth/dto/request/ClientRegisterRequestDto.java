package com.example.readys7project.domain.user.auth.dto.request;

import com.example.readys7project.domain.user.enums.ParticipateType;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record ClientRegisterRequestDto (

    @NotBlank(message = "직군 입력은 필수입니다.")
    String title,

    @NotBlank(message = "사업자 유형은 필수입니다.")
    ParticipateType participateType

) {}
