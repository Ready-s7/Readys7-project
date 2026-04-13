package com.example.readys7project.domain.user.developer.dto.request;

import com.example.readys7project.domain.user.developer.dto.validation.ValidHourlyPayRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@ValidHourlyPayRange   // 커스텀 어노테이션
public record DeveloperProfileRequestDto (

        @NotBlank(message = "직함은 필수입니다")
        String title,

        String skills,        // JSON 문자열 ex: ["React", "Spring"]

        @NotNull(message = "최소 시급은 필수입니다")
        @Positive(message = "최소 시급은 0보다 커야합니다")  // @Positive -> 양수만 허용
        Integer minHourlyPay,

        @NotNull(message = "최대 시급은 필수입니다")
        @Positive(message = "최대 시급은 0보다 커야합니다")
        Integer maxHourlyPay,

        @NotBlank(message = "응답 시간은 필수입니다")
        String responseTime,

        @NotNull(message = "근무 가능 여부는 필수입니다")
        Boolean availableForWork
) {}