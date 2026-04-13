package com.example.readys7project.domain.user.client.dto.response;

import com.example.readys7project.domain.project.enums.ProjectStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ClientProjectSummaryResponseDto (

        Long Id,

        String title,

        ProjectStatus projectStatus,

        Integer currentProposalCount,

        LocalDateTime postedDate

) {}
