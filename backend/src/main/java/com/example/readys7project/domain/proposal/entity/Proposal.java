package com.example.readys7project.domain.proposal.entity;

import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Proposal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String coverLetter;

    @Column(nullable = false)
    private String proposedBudget;

    @Column(nullable = false)
    private String proposedDuration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Builder
    public Proposal(Project project, Developer developer, String coverLetter, String proposedBudget, String proposedDuration, ProposalStatus status) {
        this.project = project;
        this.developer = developer;
        this.coverLetter = coverLetter;
        this.proposedBudget = proposedBudget;
        this.proposedDuration = proposedDuration;
        this.status = status;
    }
}
