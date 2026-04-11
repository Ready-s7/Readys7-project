package com.example.readys7project.domain.skill.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor

public enum SkillCategory {

    FRONTEND("FRONTEND"),
    BACKEND("BACKEND"),
    DEVOPS("DEVOPS"),
    GAME("GAME"),
    MOBILE("MOBILE"),
    EMBEDDED("EMBEDDED"),
    BIGDATA("BIGDATA"),
    AI("AI");

    private final String title;

}
