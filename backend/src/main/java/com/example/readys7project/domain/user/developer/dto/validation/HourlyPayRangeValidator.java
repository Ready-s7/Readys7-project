package com.example.readys7project.domain.user.developer.dto.validation;

import com.example.readys7project.domain.user.developer.dto.request.DeveloperProfileRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HourlyPayRangeValidator
        implements ConstraintValidator<ValidHourlyPayRange, HourlyPayRange> {


    @Override
    public boolean isValid(HourlyPayRange value, ConstraintValidatorContext context) {
        if (value == null) return true;

        // null 체크
        if (value.minHourlyPay() == null || value.maxHourlyPay() == null) {
            return true;       //@NotNull이 별도로 처리하므로 여기선 통과
        }
        // 최소 시급이 최대 시급보다 클 경우 -> false
        return value.minHourlyPay() <= value.maxHourlyPay();
    }
}
