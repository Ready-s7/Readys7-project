package com.example.readys7project.domain.proposal.repository;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.user.developer.entity.Developer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    Page<Proposal> findByProjectId(Long projectId, Pageable pageable);
    Page<Proposal> findByDeveloperId(Long developerId, Pageable pageable);
    Optional<Proposal> findByProjectIdAndDeveloperId(Long projectId, Long developerId);
    Optional<Proposal> findByProjectAndDeveloper(Project project, Developer developer);
}
