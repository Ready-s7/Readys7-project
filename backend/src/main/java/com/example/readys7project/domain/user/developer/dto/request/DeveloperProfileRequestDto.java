package com.example.readys7project.domain.user.developer.dto.request;

import com.example.readys7project.domain.user.developer.dto.validation.HourlyPayRange;
import com.example.readys7project.domain.user.developer.dto.validation.ValidHourlyPayRange;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;

@ValidHourlyPayRange   // 커스텀 어노테이션
public record DeveloperProfileRequestDto (

        String title,

        List<String> skills,        // JSON 문자열 ex: ["React", "Spring"]

        @Positive(message = "최소 시급은 0보다 커야합니다")  // @Positive -> 양수만 허용
        Integer minHourlyPay,

        @Positive(message = "최대 시급은 0보다 커야합니다")
        Integer maxHourlyPay,

        @Pattern(regexp = "^(\\d+(시간|분))?$", message = "형식이 올바르지 않습니다. (예: 10시간, 30분)")
        String responseTime,

        Boolean availableForWork
) implements HourlyPayRange {}