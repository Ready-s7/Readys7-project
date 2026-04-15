package com.example.readys7project.domain.review.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewRole {
    CLIENT("리뷰 고객"),
    DEVELOPER("리뷰 개발자");

    private final String writerRole;
}
