package com.example.readys7project.domain.project.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectStatus {

    OPEN("오픈"),
    CLOSED("모집 종료"),
    IN_PROGRESS("작업 중"),
    COMPLETED("작업 완료"),
    CANCELLED("작업 중단");

    private final String title;
}
