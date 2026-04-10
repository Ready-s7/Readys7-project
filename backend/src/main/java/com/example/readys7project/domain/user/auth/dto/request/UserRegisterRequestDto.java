package com.example.readys7project.domain.user.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;


@Builder
public record UserRegisterRequestDto (

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email,

    @NotBlank(message = "비밀번호는 필수입니다")
    String password,

    @NotBlank(message = "이름은 필수입니다")
    String name,

    @NotBlank(message = "역할은 필수입니다")
    String role, // CLIENT 또는 DEVELOPER

    @Pattern(
            regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$",
            message = "하이픈(-)을 제외한 올바른 전화번호 형식이어야 합니다."
    )
    String phoneNumber,

    String description
) {}