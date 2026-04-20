package com.example.readys7project.domain.chat.message.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메시지 생성 시 발생하는 이벤트
 * DB 트랜잭션이 최종 커밋된 후 Redis의 안 읽은 메시지 수를 업데이트하기 위해 사용됨.
 */
@Getter
@RequiredArgsConstructor
public class MessageCreatedEvent {
    private final Long roomId;
    private final Long receiverId;
}
