package com.example.readys7project.domain.proposal.repository;

import com.example.readys7project.domain.proposal.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByProjectId(Long projectId);
    List<Proposal> findByDeveloperId(Long developerId);
    Optional<Proposal> findByProjectIdAndDeveloperId(Long projectId, Long developerId);
}
