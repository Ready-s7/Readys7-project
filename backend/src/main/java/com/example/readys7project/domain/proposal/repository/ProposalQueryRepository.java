package com.example.readys7project.domain.proposal.repository;

import com.example.readys7project.domain.proposal.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProposalQueryRepository {
    Page<Proposal> findByProjectId(Long projectId, Pageable pageable);
    Page<Proposal> findByDeveloperId(Long developerId, Pageable pageable);
}
