package com.example.readys7project.domain.user.client.dto.response;

import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.enums.ParticipateType;
import lombok.Builder;

@Builder
public record ClientSummaryResponseDto (

        Long id,

        String name,

        String title,

        Integer completedProject,

        Double rating,

        Integer reviewCount,

        ParticipateType participateType,

        String description

) {
    public ClientSummaryResponseDto(Client client) {
        this(
                client.getId(),
                client.getUser().getName(),
                client.getTitle(),
                client.getCompletedProject(),
                client.getRating(),
                client.getReviewCount(),
                client.getParticipateType(),
                client.getUser().getDescription()
        );
    }
}
