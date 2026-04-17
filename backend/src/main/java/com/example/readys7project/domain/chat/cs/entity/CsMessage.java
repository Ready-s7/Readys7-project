package com.example.readys7project.domain.chat.cs.entity;

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
@Table(name = "cs_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE cs_messages SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class CsMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cs_chat_room_id", nullable = false)
    private CsChatRoom csChatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageEventType eventType;

    @Column(nullable = false)
    private Boolean isRead = false;

    private boolean isDeleted = false;

    @Builder
    public CsMessage(CsChatRoom csChatRoom, User sender, String content, MessageEventType eventType) {
        this.csChatRoom = csChatRoom;
        this.sender = sender;
        this.content = content;
        this.eventType = eventType;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }

    public void updateContent(String content) {
        this.content = content;
        this.eventType = MessageEventType.EDIT;
    }
}
