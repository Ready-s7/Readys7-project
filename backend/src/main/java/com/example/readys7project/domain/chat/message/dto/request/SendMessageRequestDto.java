package com.example.readys7project.domain.chat.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SendMessageRequestDto(
        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
) {}
