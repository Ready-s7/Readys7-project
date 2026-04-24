package com.example.readys7project.domain.user.auth.dto.request;

import com.example.readys7project.domain.user.developer.dto.validation.HourlyPayRange;
import com.example.readys7project.domain.user.developer.dto.validation.ValidHourlyPayRange;
import com.example.readys7project.domain.user.enums.ParticipateType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;

@Builder
@ValidHourlyPayRange
public record DeveloperRegisterRequestDto(

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

        // DEVELOPER 전용
        @NotBlank(message = "직군 입력은 필수입니다.")
        String title,

        @NotNull(message = "최소 금액 입력은 필수 입니다.")
        @Positive(message = "0보다 큰 금액을 입력해야 합니다.")
        Integer minHourlyPay,

        @NotNull(message = "최대 금액 입력은 필수 입니다.")
        @Positive(message = "0보다 큰 금액을 입력해야 합니다.")
        Integer maxHourlyPay,

        // @NotNull 만으론 빈 리스트를 못막을 수 있음
        @NotEmpty(message = "기술은 한 개 이상 입력해야합니다.")
        List<String> skills,

        @NotBlank(message = "응답시간 입력은 필수입니다.")
        @Pattern(regexp = "^(\\d+\\s*시간)?\\s*(\\d+\\s*분)?$", message = "형식이 올바르지 않습니다. (예: 1시간, 30분, 1시간 30분)")
        String responseTime,

        @NotNull(message = "일 시작 여부는 필수입니다.")
        Boolean availableForWork,

        @NotNull(message = "사업자 유형은 필수입니다.")
        ParticipateType participateType

) implements HourlyPayRange {
}
