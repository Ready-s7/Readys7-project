package com.example.readys7project.domain.project.entity;

import com.example.readys7project.domain.project.dto.request.ProjectRequest;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.util.ArrayList;
import java.util.List;

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
    private User client;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String category; // web, mobile, ai, blockchain, game, design

    private String budget;

    private String duration;

    @ElementCollection
    @CollectionTable(name = "project_skills", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(name = "proposal_count", nullable = false)
    private Integer proposalCount = 0;

    @Builder
    public Project(User client, String title, String description, String category, String budget,
                   String duration, List<String> skills, ProjectStatus projectStatus, Integer proposalCount) {
        this.client = client;
        this.title = title;
        this.description = description;
        this.category = category;
        this.budget = budget;
        this.duration = duration;
        this.skills = skills;
        this.status = projectStatus;
        this.proposalCount = proposalCount;
    }

    public void update(ProjectRequest request) {
        this.title = request.getTitle();
        this.description = request.getDescription();
        this.category = request.getCategory();
        this.budget = request.getBudget();
        this.duration = request.getDuration();
        this.skills = request.getSkills();
    }

}
