package com.example.readys7project.domain.chat.chatroom.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateChatRoomRequestDto(
        @NotNull(message = "프로젝트 ID는 필수입니다.")
        Long projectId,
        @NotNull(message = "개발자 ID는 필수입니다.")
        Long developerId
) {
}
