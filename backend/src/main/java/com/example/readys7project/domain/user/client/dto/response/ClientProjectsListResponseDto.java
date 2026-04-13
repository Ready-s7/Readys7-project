package com.example.readys7project.domain.user.client.dto.response;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ClientProjectsListResponseDto(

        Long Id,

        String title,

        ProjectStatus projectStatus,

        Integer currentProposalCount,

        LocalDateTime postedDate

) {
    public static ClientProjectsListResponseDto from(Project project) {
        return ClientProjectsListResponseDto.builder()
                .Id(project.getId())
                .title(project.getTitle())
                .projectStatus(project.getStatus())
                .currentProposalCount(project.getCurrentProposalCount())
                .postedDate(project.getCreatedAt())
                .build();
    }
}
