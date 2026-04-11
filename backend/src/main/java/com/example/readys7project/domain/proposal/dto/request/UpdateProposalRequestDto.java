package com.example.readys7project.domain.proposal.dto.request;

import com.example.readys7project.domain.proposal.enums.ProposalStatus;

public record UpdateProposalRequestDto(
        ProposalStatus status
) {
}
