package com.example.readys7project.global.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 리소스 소유자 또는 관리자 권한을 체크하는 어노테이션
 * - type: 엔티티 종류 (PROJECT, PROPOSAL, REVIEW 등)
 * - idParam: 메소드 파라미터 중 리소스 ID 값 (기본값 "id")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckOwnerOrAdmin {
    EntityType type();
    String idParam() default "id";
}
