package com.example.readys7project.domain.client.enums;

import lombok.Getter;

@Getter
public enum ParticipateType {

    INDIVIDUAL("개인"),
    COMPANY("회사");

    private final String type;

    ParticipateType(String type) {
        this.type = type;
    }
}
