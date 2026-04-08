package com.example.readys7project.domain.proposal.controller;

import com.example.readys7project.domain.proposal.dto.ProposalDto;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequest;
import com.example.readys7project.domain.proposal.service.ProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @PostMapping
    public ResponseEntity<ProposalDto> createProposal(
            @Valid @RequestBody ProposalRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(proposalService.createProposal(request, email));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProposalDto>> getProposalsByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(proposalService.getProposalsByProject(projectId));
    }

    @GetMapping("/my-proposals")
    public ResponseEntity<List<ProposalDto>> getMyProposals(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(proposalService.getProposalsByDeveloper(email));
    }

    @PatchMapping("/{proposalId}/status")
    public ResponseEntity<ProposalDto> updateProposalStatus(
            @PathVariable Long proposalId,
            @RequestParam String status,
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(proposalService.updateProposalStatus(proposalId, status, email));
    }
}
