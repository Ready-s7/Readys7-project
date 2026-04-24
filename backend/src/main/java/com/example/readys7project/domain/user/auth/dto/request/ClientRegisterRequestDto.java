package com.example.readys7project.domain.user.auth.dto.request;

import com.example.readys7project.domain.user.enums.ParticipateType;
import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record ClientRegisterRequestDto(

        // 공통 필드
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다")
        String password,

        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다")
        String name,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(
                regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$",
                message = "하이픈(-)을 제외한 올바른 전화번호 형식이어야 합니다."
        )
        String phoneNumber,

        String description,

        // CLIENT 전용
        @NotBlank(message = "직군 입력은 필수입니다.")
        String title,

        @NotNull(message = "사업자 유형은 필수입니다.")
        ParticipateType participateType

) {
}
