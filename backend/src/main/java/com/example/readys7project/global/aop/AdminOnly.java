package com.example.readys7project.global.aop;

import com.example.readys7project.domain.user.admin.enums.AdminRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 관리자 권한 및 승인 상태를 검증하는 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOnly {
    /**
     * 허용되는 최소 관리자 등급 (기본값은 모든 관리자 허용을 위해 null 대용으로 사용될 수 있는 값 설정 가능)
     * 여기서는 명시적인 역할을 지정할 수 있도록 AdminRole을 인자로 받습니다.
     */
    AdminRole role() default AdminRole.OPER_ADMIN; 
}
