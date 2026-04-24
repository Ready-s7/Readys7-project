package com.example.readys7project.domain.chat.message.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MessageEventType {
    SEND("전송"),
    EDIT("수정"),
    DELETE("삭제"),
    ENTER("입장"),
    LEAVE("퇴장");

    private final String title;
}
