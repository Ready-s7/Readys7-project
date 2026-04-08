package com.example.readys7project.domain.proposal.entity;

import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "proposals")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {

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

    @CreatedDate
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    public enum ProposalStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        WITHDRAWN
    }
}
