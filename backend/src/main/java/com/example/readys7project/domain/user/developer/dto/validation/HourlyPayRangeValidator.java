package com.example.readys7project.domain.user.developer.dto.validation;

import com.example.readys7project.domain.user.developer.dto.request.DeveloperProfileRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HourlyPayRangeValidator
        implements ConstraintValidator<ValidHourlyPayRange, DeveloperProfileRequestDto> {


    @Override
    public boolean isValid(DeveloperProfileRequestDto dto, ConstraintValidatorContext context) {
        // null 체크
        if (dto.minHourlyPay() == null || dto.maxHourlyPay() == null) {
            return true;       //@NotNull이 별도로 처리하므로 여기선 통과
        }
        // 최소 시급이 최대 시급보다 클 경우 -> false
        return dto.minHourlyPay() <= dto.maxHourlyPay();
    }
}
