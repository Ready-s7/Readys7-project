package com.example.readys7project.domain.client.entity;

import com.example.readys7project.domain.client.enums.ParticipateType;
import com.example.readys7project.domain.user.entity.User;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "clients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 5)
    private int completedProject;

    @Column(nullable = false)
    private Double rating;

    @Column(length = 5)
    private int reviewCount;

    @Enumerated(EnumType.STRING)
    private ParticipateType participateType;

    @Builder
    public Client(User user, String title, int completedProject, Double rating, int reviewCount, ParticipateType participateType) {
        this.user = user;
        this.title = title;
        this.completedProject = completedProject;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.participateType = participateType;
    }

    // 참여 타입 수정
    public void update(ParticipateType participateType) {
        this.participateType = participateType;
    }

}
