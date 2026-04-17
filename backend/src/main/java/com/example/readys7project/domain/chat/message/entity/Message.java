package com.example.readys7project.domain.chat.message.entity;

import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.chat.message.enums.MessageEventType;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE messages SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User user;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private MessageEventType eventType;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "is_system", nullable = false)
    private Boolean isSystem = false;  // 시스템 메시지 여부

    private boolean isDeleted = false;

    @Builder
    public Message (ChatRoom chatRoom, User user, String content, MessageEventType eventType, Boolean isSystem) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.content = content;
        this.eventType = eventType;
        this.isRead = false;
        this.isSystem = isSystem != null && isSystem;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}
