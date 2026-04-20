package com.example.readys7project.domain.chat.message.event;

import com.example.readys7project.domain.chat.message.service.UnreadCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 메시지 생성 이벤트 핸들러
 * DB 트랜잭션이 최종 커밋된 후(AFTER_COMMIT) Redis의 unread count를 업데이트함.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnreadCountEventHandler {

    private final UnreadCountService unreadCountService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMessageCreated(MessageCreatedEvent event) {
        try {
            log.info("DB 커밋 감지: Redis unread count 업데이트 (Room: {}, Receiver: {})", 
                    event.getRoomId(), event.getReceiverId());
            
            unreadCountService.increment(event.getRoomId(), event.getReceiverId());
            
        } catch (Exception e) {
            log.error("Redis unread count 업데이트 중 에러 발생 (Room: {}, User: {}): {}", 
                    event.getRoomId(), event.getReceiverId(), e.getMessage());
            // 필요 시 재시도 로직이나 후속 처리 추가 가능
        }
    }
}
