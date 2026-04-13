package com.example.readys7project.domain.user.developer.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = HourlyPayRangeValidator.class)   // 검증로직 담당 지정
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)                        // @Retention 이 없으면 실행 중에 어노테이션을 읽지 못함
public @interface ValidHourlyPayRange {

    String message() default "최소 시급은 최대 시급보다 클 수 없습니다";
    Class<?>[] groups() default {};                              // <?> -> 어떤 타입의 클래스든 허용 (? = 와일드카드)
    Class<? extends Payload>[] payload() default {};             // <? extends Payload> -> Payload를 상속한 클래스만 허용
}
