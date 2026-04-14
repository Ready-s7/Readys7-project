package com.example.readys7project.domain.user.client.dto.request;

import com.example.readys7project.domain.user.enums.ParticipateType;

public record UpdateClientProfileRequestDto(

        String title,

        ParticipateType participateType

) {}
