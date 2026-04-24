package com.example.readys7project.domain.chat.cs.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CsChatRoomRequestDto(
    @NotBlank(message = "제목은 필수입니다.")
    String title
) {}
