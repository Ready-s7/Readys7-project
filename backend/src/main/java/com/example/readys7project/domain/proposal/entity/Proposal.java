package com.example.readys7project.domain.proposal.entity;

import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.entity.BaseEntity;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ProposalException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SoftDelete;

@Getter
@Entity
@Table(name = "proposals")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SoftDelete
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

    public void  updateStatus(ProposalStatus status, UserRole role) {

        // 'client가 승인 or 거절하는 경우'이거나
        if (!((role.equals(UserRole.CLIENT) && (status.equals(ProposalStatus.ACCEPTED) || status.equals(ProposalStatus.REJECTED)))
                    // 'developer가 철회하는 경우'이거나
                || (role.equals(UserRole.DEVELOPER) && status.equals(ProposalStatus.WITHDRAWN))
                    // '관리자인 경우'가 "아니라면" 예외 처리
                || (role.equals(UserRole.ADMIN))
        )) {
            throw new ProposalException(ErrorCode.USER_FORBIDDEN);
        }

        this.status = status;
    }
}
