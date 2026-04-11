package com.example.readys7project.global.exception.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    USER_INFO_MISMATCH(HttpStatus.BAD_REQUEST, "유저의 정보가 일치하지 않습니다."),
    USER_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    USER_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 유저입니다."),

    // Developer
    DEVELOPER_NOT_FOUND(HttpStatus.NOT_FOUND, "개발자를 찾을 수 없습니다."),

    // Client
    CLIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "클라이언트를 찾을 수 없습니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND,"카테고리를 찾을 수 없습니다."),

    // Proposal
    PROPOSAL_NOT_FOUND(HttpStatus.NOT_FOUND, "제안서를 찾을 수 없습니다."),
    PROPOSAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 해당 프로젝트에 제안서를 제출했습니다."),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Admin
    INVALID_ADMIN_ROLE(HttpStatus.UNAUTHORIZED, "잘못된 역할입니다."),
    ADMIN_STATUS_NOT_MATCH(HttpStatus.BAD_REQUEST, "승인 대기중 상태가 아닙니다."),
    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다"),
    ADMIN_ALREADY_APPROVE(HttpStatus.BAD_REQUEST, "이미 승인된 관리자 입니다.");

    private final HttpStatus status;
    private final String message;
}
