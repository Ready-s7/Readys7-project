package com.example.readys7project.domain.user.client.entity;

import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.enums.ParticipateType;
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
@Table(name = "clients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE clients SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
public class Client extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "completed_project", nullable = false, length = 5)
    private Integer completedProject;

    @Column(name = "rating", nullable = false)
    private Double rating;

    @Column(name = "review_count", length = 5)
    private Integer reviewCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "participate_type", nullable = false, length = 20)
    private ParticipateType participateType;


    private boolean isDeleted = false;


    @Builder
    public Client(User user, String title, Integer completedProject, Double rating,
                  Integer reviewCount, ParticipateType participateType) {
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

    // 클라이언트 프로필 수정
    public void updateProfile(String title, ParticipateType participateType) {
        this.title = title;
        this.participateType = participateType;
    }

}
