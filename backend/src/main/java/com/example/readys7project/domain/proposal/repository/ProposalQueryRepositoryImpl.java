package com.example.readys7project.domain.proposal.repository;

import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.entity.QProposal;
import com.example.readys7project.domain.user.developer.entity.QDeveloper;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProposalQueryRepositoryImpl implements ProposalQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    private final QProposal qProposal = QProposal.proposal;
    private final QProject qProject = QProject.project;
    private final QDeveloper qDeveloper = QDeveloper.developer;
    private final QClient qClient = QClient.client;
    
    // Alias conflict prevention
    private final QUser developerUser = new QUser("developerUser");
    private final QUser clientUser = new QUser("clientUser");

    @Override
    public Page<Proposal> findByProjectId(Long projectId, Pageable pageable) {
        List<Proposal> content = jpaQueryFactory
                .selectFrom(qProposal)
                .join(qProposal.project, qProject).fetchJoin()
                .join(qProposal.developer, qDeveloper).fetchJoin()
                .join(qDeveloper.user, developerUser).fetchJoin()
                .leftJoin(qProject.client, qClient).fetchJoin()
                .leftJoin(qClient.user, clientUser).fetchJoin()
                .where(qProposal.project.id.eq(projectId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qProposal.createdAt.desc())
                .fetch();

        Long total = jpaQueryFactory
                .select(qProposal.count())
                .from(qProposal)
                .where(qProposal.project.id.eq(projectId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }

    @Override
    public Page<Proposal> findByDeveloperId(Long developerId, Pageable pageable) {
        List<Proposal> content = jpaQueryFactory
                .selectFrom(qProposal)
                .join(qProposal.project, qProject).fetchJoin()
                .join(qProposal.developer, qDeveloper).fetchJoin()
                .join(qDeveloper.user, developerUser).fetchJoin()
                .leftJoin(qProject.client, qClient).fetchJoin()
                .leftJoin(qClient.user, clientUser).fetchJoin()
                .where(qProposal.developer.id.eq(developerId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qProposal.createdAt.desc())
                .fetch();

        Long total = jpaQueryFactory
                .select(qProposal.count())
                .from(qProposal)
                .where(qProposal.developer.id.eq(developerId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
