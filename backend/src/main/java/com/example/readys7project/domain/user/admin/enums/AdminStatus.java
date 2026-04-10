package com.example.readys7project.domain.user.admin.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminStatus {

    PENDING("승인 대기"),
    APPROVED("승인 완료"),
    REJECTED("승인 취소");

    private final String title;
}
