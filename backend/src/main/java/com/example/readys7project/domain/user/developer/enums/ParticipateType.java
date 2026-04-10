package com.example.readys7project.domain.user.developer.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ParticipateType {
    INDIVIDUAL("개인"),
    COMPANY("회사");

    private final String title;
}
