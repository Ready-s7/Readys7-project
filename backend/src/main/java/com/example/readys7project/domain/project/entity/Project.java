package com.example.readys7project.domain.project.entity;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "json")
    private String skills;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category; // web, mobile, ai, blockchain, game, design

    @Column(nullable = false)
    private Integer budget;   // String 타입으로 받을시 검색/필터링/정렬 전부 불가능 // 단위: 만원 or 원
    @Column(nullable = false) // 그래서 String에서 Integer 타입으로 변경
    private Integer duration; // 마찬가지

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(nullable = false)
    private Integer maxProposals;          // 동시성 제어 필드 - 모집정원 (고정값)

    @Column(nullable = false)
    private Integer currentProposals = 0;  // 동시성 제어 필드 - 현재 지원자 수 (경쟁자원)

    @Column(nullable = false)
    private Boolean recruiting = true;     // 동시성 제어 필드 - 모집 중 여부 (빠른 조회용 상태값)

    @Column(nullable = false)
    private LocalDateTime recruitDeadline; // 동시성 제어 필드 - 시간 기반 이벤트

    @Builder
    public Project(Client client, String title, String description, String skills, Category category,
                   Integer budget, Integer duration, Integer maxProposals, LocalDateTime recruitDeadline) {
        this.client = client;
        this.title = title;
        this.description = description;
        this.skills = skills;
        this.category = category;
        this.budget = budget;
        this.duration = duration;

        // 초기화 세팅
        this.status = ProjectStatus.OPEN;
        this.maxProposals = maxProposals;
        this.currentProposals = 0;
        this.recruiting = true;
        this.recruitDeadline = recruitDeadline;
    }

    public void update(
            String title,
            String description,
            Category category,
            String skills,
            Integer budget,
            Integer duration,
            Integer maxProposals,
            LocalDateTime recruitDeadline
    ) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.skills = skills;
        this.budget = budget;
        this.duration = duration;
        this.maxProposals = maxProposals;
        this.recruitDeadline = recruitDeadline;
    }

    public void increaseProposalCount() {
        this.currentProposals++;

        if (this.currentProposals >= this.maxProposals) {
            this.recruiting = false;
            this.status = ProjectStatus.CLOSED;
        }
    }

}
