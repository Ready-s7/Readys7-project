package com.example.readys7project.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    CLIENT("고객"),
    DEVELOPER("개발 인력");

    private final String title;
}
