package com.example.readys7project.domain.user.client.dto.response;

import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.enums.ParticipateType;
import lombok.Builder;

@Builder
public record ClientsResponseDto(

        Long id,

        String name,

        String title,

        Integer completedProject,

        Double rating,

        Integer reviewCount,

        ParticipateType participateType,

        String description

) {
    public static ClientsResponseDto from(Client client) {
        return ClientsResponseDto.builder()
                .id(client.getId())
                .name(client.getUser().getName())
                .title(client.getTitle())
                .completedProject(client.getCompletedProject())
                .rating(client.getRating())
                .participateType(client.getParticipateType())
                .description(client.getUser().getDescription())
                .build();
    }
}
