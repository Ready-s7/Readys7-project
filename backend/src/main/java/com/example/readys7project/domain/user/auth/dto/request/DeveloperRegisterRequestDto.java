package com.example.readys7project.domain.user.auth.dto.request;

import com.example.readys7project.domain.user.enums.ParticipateType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;

@Builder
public record DeveloperRegisterRequestDto (

        @NotBlank(message = "직군 입력은 필수입니다.")
        String title,

        @NotNull(message = "최소 금액 입력은 필수 입니다.")
        @Positive(message = "0보다 작은 금액은 입력할 수 없습니다.")
        Integer minHourlyPay,

        @NotNull(message = "최대 금액 입력은 필수 입니다.")
        @Positive(message = "0보다 작은 금액은 입력할 수 없습니다.")
        Integer maxHourlyPay,

        @NotBlank(message = "기술은 한 개 이상 입력해야합니다.")
        List<String> skills,

        @NotBlank(message = "일 시작 여부는 필수입니다.")
        Boolean availableForWork,

        @NotBlank(message = "사업자 유형은 필수입니다.")
        ParticipateType participateType

){}
