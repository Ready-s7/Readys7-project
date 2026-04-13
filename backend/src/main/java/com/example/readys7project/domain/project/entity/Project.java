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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Getter
@Table(name = "projects")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE projects SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
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
    private Integer minBudget; // 최저예산, String 타입으로 받을시 검색/필터링/정렬 전부 불가능 // 단위: 만원 or 원
    @Column(nullable = false)
    private Integer maxBudget; // 최대예산

    @Column(nullable = false) // 그래서 String에서 Integer 타입으로 변경
    private Integer duration; // 마찬가지

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(nullable = false)
    private Integer currentProposalCount; // 현재 제안서 수

    @Column(nullable = false)
    private Integer maxProposalCount;     // 최대 제안서 수

    private boolean isDeleted = false;

    @Builder
    public Project(Client client, String title, String description, String skills, Category category,
                   Integer minBudget,Integer maxBudget, Integer duration, Integer maxProposalCount) {
        this.client = client;
        this.title = title;
        this.description = description;
        this.skills = skills;
        this.category = category;
        this.minBudget = minBudget;
        this.maxBudget = maxBudget;
        this.duration = duration;
        this.maxProposalCount = maxProposalCount;

        // 초기화 세팅
        this.status = ProjectStatus.OPEN;
        this.currentProposalCount = 0;

    }

    public void update(
            String title,
            String description,
            Category category,
            String skills,
            Integer minBudget,
            Integer maxBudget,
            Integer duration,
            Integer maxProposalCount
    ) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.skills = skills;
        this.minBudget = minBudget;
        this.maxBudget = maxBudget;
        this.duration = duration;
        this.maxProposalCount = maxProposalCount;

    }

    public void increaseProposalCount() {
        this.currentProposalCount++;

        if (this.currentProposalCount >= this.maxProposalCount) {
            this.status = ProjectStatus.CLOSED;
        }
    }

}
