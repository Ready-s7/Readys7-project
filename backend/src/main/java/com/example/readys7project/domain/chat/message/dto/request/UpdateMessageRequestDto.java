package com.example.readys7project.domain.chat.message.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateMessageRequestDto(
        @NotBlank
        String content
) {
}
