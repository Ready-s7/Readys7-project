package com.example.readys7project.domain.chat.cs.entity;

import com.example.readys7project.domain.chat.cs.enums.CsStatus;
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
@Table(name = "cs_chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE cs_chat_rooms SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class CsChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquirer_id", nullable = false)
    private User inquirer; // 문의자 (Client or Developer)

    @Column(nullable = false)
    private String title; // 문의 제목

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CsStatus status;

    private boolean isDeleted = false;

    @Builder
    public CsChatRoom(User inquirer, String title) {
        this.inquirer = inquirer;
        this.title = title;
        this.status = CsStatus.WAITING;
    }

    public void updateStatus(CsStatus status) {
        this.status = status;
    }
}
