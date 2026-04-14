package com.example.readys7project.domain.proposal.controller;

import com.example.readys7project.domain.proposal.dto.ProposalDto;
import com.example.readys7project.domain.proposal.dto.request.ProposalRequestDto;
import com.example.readys7project.domain.proposal.dto.request.UpdateProposalRequestDto;
import com.example.readys7project.domain.proposal.service.ProposalService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;

    @PostMapping("/v1/proposals")
    public ResponseEntity<ApiResponseDto<ProposalDto>> createProposal(
            @Valid @RequestBody ProposalRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, proposalService.createProposal(request, email))
        );
    }

    @GetMapping("/v1/proposals")
    public ResponseEntity<ApiResponseDto<Page<ProposalDto>>> getProposalsByProject(
            @RequestParam Long projectId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Pageable pageable
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity
                .ok(ApiResponseDto.success(HttpStatus.OK, proposalService.getProposalsByProject(projectId, email, pageable)));
    }

    @GetMapping("/v1/proposals/{proposalId}")
    public ResponseEntity<ApiResponseDto<ProposalDto>> getProposal(
            @PathVariable Long proposalId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, proposalService.getProposal(proposalId, email)));
    }

    @GetMapping("/v1/proposals/my-proposals")
    public ResponseEntity<ApiResponseDto<Page<ProposalDto>>> getMyProposals(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Pageable pageable
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity
                .ok(ApiResponseDto.success(HttpStatus.OK, proposalService.getMyProposals(email, pageable)));
    }

    @PatchMapping(value = "/v1/proposals/{proposalId}")
    public ResponseEntity<ApiResponseDto<ProposalDto>> updateProposalStatus(
            @PathVariable Long proposalId,
            @RequestBody UpdateProposalRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity
                .ok(ApiResponseDto.success(
                        HttpStatus.OK,
                        proposalService.updateProposalStatus(proposalId, request, email)
                ));
    }
}
