package com.example.readys7project.domain.chat.chatroom.repository;

import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.chat.chatroom.entity.QChatRoom;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.example.readys7project.domain.user.developer.entity.QDeveloper;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
public class ChatRoomQueryRepositoryImpl implements ChatRoomQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<ChatRoom> findMyChatRooms(User user, Pageable pageable) {
        QChatRoom qChatRoom = QChatRoom.chatRoom;
        QClient qClient = QClient.client;
        QDeveloper qDeveloper = QDeveloper.developer;
        QUser qClientUser = new QUser("clientUser");    // alias 구분
        QUser qDeveloperUser = new QUser("developerUser"); // alias 구분

        BooleanExpression condition = qClientUser.id.eq(user.getId())
                .or(qDeveloperUser.id.eq(user.getId()));

        List<ChatRoom> content = queryFactory
                .selectFrom(qChatRoom)
                .join(qChatRoom.client, qClient).fetchJoin()
                .join(qChatRoom.developer, qDeveloper).fetchJoin()
                .join(qClient.user, qClientUser).fetchJoin()
                .join(qDeveloper.user, qDeveloperUser).fetchJoin()
                .where(condition)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qChatRoom.count())
                .from(qChatRoom)
                .join(qChatRoom.client, qClient)
                .join(qChatRoom.developer, qDeveloper)
                .join(qClient.user, qClientUser)
                .join(qDeveloper.user, qDeveloperUser)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
