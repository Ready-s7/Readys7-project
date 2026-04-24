package com.example.readys7project.domain.proposal.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProposalStatus {

    PENDING("승인 대기"),
    ACCEPTED("승인 허가"),
    REJECTED("승인 거부"),
    WITHDRAWN("제안 철회");

    private final String title;
}