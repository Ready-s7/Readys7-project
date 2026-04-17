package com.example.readys7project.domain.proposal.controller;

import com.example.readys7project.domain.proposal.dto.response.ProposalResponseDto;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.request.UpdateProposalRequestDto;
import com.example.readys7project.domain.proposal.service.ProposalService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import com.example.readys7project.global.aop.CheckOwnerOrAdmin;
import com.example.readys7project.global.aop.EntityType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @PostMapping("/v1/proposals")
    public ResponseEntity<ApiResponseDto<ProposalResponseDto>> createProposal(
            @Valid @RequestBody ProposalRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, proposalService.createProposal(request, email))
        );
    }

    @GetMapping("/v1/proposals")
    @CheckOwnerOrAdmin(type = EntityType.PROJECT, idParam = "projectId")
    public ResponseEntity<ApiResponseDto<Page<ProposalResponseDto>>> getProposalsByProject(
            @RequestParam Long projectId,
            Pageable pageable
    ) {
        return ResponseEntity
                .ok(ApiResponseDto.success(HttpStatus.OK, proposalService.getProposalsByProject(projectId, pageable)));
    }

    @GetMapping("/v1/proposals/{proposalId}")
    @CheckOwnerOrAdmin(type = EntityType.PROPOSAL, idParam = "proposalId")
    public ResponseEntity<ApiResponseDto<ProposalResponseDto>> getProposal(
            @PathVariable Long proposalId
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, proposalService.getProposal(proposalId)));
    }

    @GetMapping("/v1/proposals/my-proposals")
    public ResponseEntity<ApiResponseDto<Page<ProposalResponseDto>>> getMyProposals(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Pageable pageable
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity
                .ok(ApiResponseDto.success(HttpStatus.OK, proposalService.getMyProposals(email, pageable)));
    }

    @PatchMapping(value = "/v1/proposals/{proposalId}")
    @CheckOwnerOrAdmin(type = EntityType.PROPOSAL, idParam = "proposalId")
    public ResponseEntity<ApiResponseDto<ProposalResponseDto>> updateProposalStatus(
            @PathVariable Long proposalId,
            @RequestBody UpdateProposalRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ResponseEntity
                .ok(ApiResponseDto.success(
                        HttpStatus.OK,
                        proposalService.updateProposalStatus(proposalId, request, customUserDetails.getEmail())
                ));
    }
}
