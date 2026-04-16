package com.example.readys7project.domain.chat.cs.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CsStatus {
    WAITING("대기중"),
    IN_PROGRESS("처리중"),
    COMPLETED("처리완료");

    private final String description;
}
