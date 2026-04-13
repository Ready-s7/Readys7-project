package com.example.readys7project.domain.user.developer.entity;

import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.enums.ParticipateType;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;


@Getter
@Entity
@Table(name = "developers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SoftDelete
public class Developer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String title; // 예: "풀스택 개발자"

    @Column(nullable = false)
    private Double rating = 0.0;   // 평점

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;   // 리뷰 갯수

    @Column(name = "completed_projects", nullable = false)
    private Integer completedProjects = 0;   // 완료된 프로젝트

    @Column(name = "min_hourly_pay")
    private Integer minHourlyPay;   // 시간당 요금

    @Column(name = "max_hourly_pay")
    private Integer maxHourlyPay;   // 시간당 요금

    @Column(columnDefinition = "json")
    private String skills;   // 개발자 보유 스킬

    @Column(name = "response_time")
    private String responseTime;   // 응답 시간 (예: "1시간 이내")

    @Column(name = "available_for_work", nullable = false)
    private Boolean availableForWork = true;   // 작업 진행 가능 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "participate_type")
    private ParticipateType participateType;   // 개발자 유형 (개인 or 회사)



    @Builder
    public Developer(User user, String title, Double rating, Integer reviewCount,
                     Integer completedProjects, String skills, Integer minHourlyPay, Integer maxHourlyPay,
                     String responseTime, Boolean availableForWork, ParticipateType participateType
    ) {
        this.user = user;
        this.title = title;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.completedProjects = completedProjects;
        this.minHourlyPay = minHourlyPay;
        this.maxHourlyPay = maxHourlyPay;
        this.skills = skills;
        this.responseTime = responseTime;
        this.availableForWork = availableForWork;
        this.participateType = participateType;
    }


    // 프로필 수정 (DEVELOPER 전용)
    public void updateProfile(String title, String skills, Integer minHourlyPay, Integer maxHourlyPay, String responseTime,
                              Boolean availableForWork
    ) {
        this.title = title;
        this.skills = skills;
        this.minHourlyPay = minHourlyPay;
        this.maxHourlyPay = maxHourlyPay;
        this.responseTime = responseTime;
        this.availableForWork = availableForWork;
    }

    // 평점 업데이트
    public void updateRating(Double rating, Integer reviewCount) {
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

}
