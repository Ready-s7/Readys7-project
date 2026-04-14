package com.example.readys7project.domain.user.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserInformationRequestDto (

        @NotBlank(message = "이름은 필수입니다")
        String name,

        @Pattern(
                regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$",
                message = "하이픈(-)을 제외한 올바른 전화번호 형식이어야 합니다."
        )
        String phoneNumber,

        String description
) {}
