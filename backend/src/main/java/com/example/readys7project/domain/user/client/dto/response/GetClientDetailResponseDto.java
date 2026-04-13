package com.example.readys7project.domain.user.client.dto.response;

import com.example.readys7project.domain.user.enums.ParticipateType;
import lombok.Builder;

@Builder
public record GetClientDetailResponseDto (

        Long id,

        String name,

        String title,

        Integer completedProject,

        Double rating,

        Integer reviewCount,

        ParticipateType participateType,

        String description

){}
