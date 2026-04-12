package com.example.readys7project.domain.chat.message.entity;

import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;

@Getter
@Entity
@Table(name = "messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SoftDelete
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

    @Column(nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isSystem = false;  // 시스템 메시지 여부

    @Builder
    public Message (ChatRoom chatRoom, User user, String content, Boolean isSystem) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.content = content;
        this.isRead = false;
        this.isSystem = isSystem != null && isSystem;
    }
}
