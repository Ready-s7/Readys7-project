package com.example.readys7project.domain.chat.message.repository;

import com.example.readys7project.domain.chat.message.entity.Message;
import com.example.readys7project.domain.chat.message.entity.QMessage;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageQueryRepositoryImpl implements MessageQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Message> findMessages(Long roomId, Long lastMessageId, Pageable pageable) {
        QMessage qMessage = QMessage.message;
        QUser qUser = new QUser("senderUser");

        return queryFactory
                .selectFrom(qMessage)
                .join(qMessage.user, qUser).fetchJoin()
                .where(
                        qMessage.chatRoom.id.eq(roomId),
                        lastMessageIdCondition(qMessage, lastMessageId)
                )
                .orderBy(qMessage.id.desc())
                .limit(pageable.getPageSize() + 1)
                .fetch();
    }

    // 최초 조회(lastMessageId == null)면 조건 무시
    // 이후 조회면 해당 id보다 작은 메시지만 조회 (커서 기반)
    private BooleanExpression lastMessageIdCondition(QMessage qMessage, Long lastMessageId) {
        if (lastMessageId == null) {
            return null;
        }
        return qMessage.id.lt(lastMessageId);
    }
}
