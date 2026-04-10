package com.example.readys7project.domain.user.developer.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Builder
public record DeveloperProfileRequestDto (

        @NotBlank(message = "직함은 필수입니다")
        String title,

        String skills,        // JSON 문자열 ex) ["React", "Spring"]
        Integer minHourlyPay,
        Integer maxHourlyPay,
        String responseTime,
        Boolean availableForWork
) {}
