package com.example.readys7project.domain.project.entity;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.global.converter.StringListConverter;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;


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

    @Column(nullable = false, name = "title")
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false, name = "description")
    private String description;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "json", name = "skills")
    private List<String> skills;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category; // web, mobile, ai, blockchain, game, design

    @Column(nullable = false, name = "min_budget")
    private Long minBudget; // 최저예산, String 타입으로 받을시 검색/필터링/정렬 전부 불가능 // 단위: 만원 or 원
    @Column(nullable = false, name = "max_budget")
    private Long maxBudget; // 최대예산

    @Column(nullable = false, name = "duration") // 그래서 String에서 Integer 타입으로 변경
    private Integer duration; // 마찬가지

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "project_status")
    private ProjectStatus status;

    @Column(nullable = false, name = "current_proposal_count")
    private Integer currentProposalCount; // 현재 제안서 수

    @Column(nullable = false, name = "max_proposal_count")
    private Integer maxProposalCount;     // 최대 제안서 수

    @Column(columnDefinition = "TEXT", name = "skills_text")
    private String skillsText; // 검색 성능 향상을 위한 섀도우 컬럼

    private boolean isDeleted = false;

    @Builder
    public Project(Client client, String title, String description, List<String> skills, Category category,
                   Long minBudget, Long maxBudget, Integer duration, Integer maxProposalCount) {
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
        
        // skillsText 초기화
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

    public void updateProject(
            String title,
            String description,
            Category category,
            List<String> skills,
            Long minBudget,
            Long maxBudget,
            Integer duration,
            Integer maxProposalCount
    ) {
        if (title != null && !title.isBlank()) this.title = title;
        if (description != null && !description.isBlank()) this.description = description;
        if (category != null) this.category = category;
        if (skills != null) {
            this.skills = skills;
            this.syncSkillsText(); // skills 변경 시 skillsText 동기화
        }
        if (minBudget != null) this.minBudget = minBudget;
        if (maxBudget != null) this.maxBudget = maxBudget;
        if (duration != null) this.duration = duration;
        if (maxProposalCount != null) this.maxProposalCount = maxProposalCount;

    }

    public void increaseProposalCount() {
        this.currentProposalCount++;

        if (this.currentProposalCount >= this.maxProposalCount) {
            this.status = ProjectStatus.CLOSED;
        }
    }

    public void decreaseProposalCount() {
        if (this.currentProposalCount > 0) {
            this.currentProposalCount--;
        }
    }

    // 프로젝트 상태 변경 로직
    public void changeStatus(ProjectStatus newStatus) {
        this.status = newStatus;
    }
}
