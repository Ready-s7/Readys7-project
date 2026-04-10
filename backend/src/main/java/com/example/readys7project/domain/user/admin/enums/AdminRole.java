package com.example.readys7project.domain.user.admin.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminRole {

    SUPER_ADMIN("SUPER_ADMIN"),
    CS_ADMIN("CS_ADMIN"),
    OPER_ADMIN("OPER_ADMIN");

    private final String title;
}
