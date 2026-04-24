package com.example.readys7project.domain.user.developer.entity;

import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.enums.ParticipateType;
import com.example.readys7project.global.converter.StringListConverter;
import com.example.readys7project.global.entity.BaseEntity;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.DeveloperException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;


@Getter
@Entity
@Table(name = "developers")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE developers SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
public class Developer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version = 0L;  // 낙관적 락

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

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "json")
    private List<String> skills;   // 개발자 보유 스킬

    @Column(columnDefinition = "TEXT", name = "skills_text")
    private String skillsText; // 검색 성능 향상을 위한 섀도우 컬럼

    @Column(name = "response_time")
    private String responseTime;   // 응답 시간 (예: "1시간 이내")

    @Column(name = "available_for_work", nullable = false)
    private Boolean availableForWork = true;   // 작업 진행 가능 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "participate_type")
    private ParticipateType participateType;   // 개발자 유형 (개인 or 회사)

    private boolean isDeleted = false;


    @Builder
    public Developer(User user, String title, Double rating, Integer reviewCount,
                     Integer completedProjects, List<String> skills, Integer minHourlyPay, Integer maxHourlyPay,
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

        // 시급 범위 검증: 생성 시에도 정합성 체크
        if (this.minHourlyPay != null && this.maxHourlyPay != null && this.minHourlyPay > this.maxHourlyPay) {
            throw new DeveloperException(ErrorCode.DEVELOPER_PAY_RANGE_INVALID);
        }

        this.syncSkillsText();
    }

    @PrePersist
    @PreUpdate
    public void syncSkillsText() {
        if (this.skills != null && !this.skills.isEmpty()) {
            this.skillsText = String.join(" ", this.skills);
        } else {
            this.skillsText = "";
        }
    }


    // 프로필 수정 (DEVELOPER 전용)
    public void updateProfile(String title, List<String> skills, Integer minHourlyPay, Integer maxHourlyPay, String responseTime,
                              Boolean availableForWork
    ) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (skills != null && !skills.isEmpty()) {
            this.skills = skills;
            this.syncSkillsText();
        }
        if (minHourlyPay != null) {
            this.minHourlyPay = minHourlyPay;
        }
        if (maxHourlyPay != null) {
            this.maxHourlyPay = maxHourlyPay;
        }
        if (responseTime != null && !responseTime.isBlank()) {
            this.responseTime = responseTime;
        }
        if (availableForWork != null) {
            this.availableForWork = availableForWork;
        }

        // 시급 범위 검증: 최소 시급이 최대 시급보다 크면 예외 발생
        if (this.minHourlyPay != null && this.maxHourlyPay != null && this.minHourlyPay > this.maxHourlyPay) {
            throw new DeveloperException(ErrorCode.DEVELOPER_PAY_RANGE_INVALID);
        }
    }

    // 평점 업데이트
    public void updateRating(Double rating, Integer reviewCount) {
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

}
