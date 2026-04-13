package com.example.readys7project.domain.skill.dto.request;

import com.example.readys7project.domain.skill.enums.SkillCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateSkillRequestDto(
        @NotBlank(message = "이름을 입력해주세요.")
        String name,
        @NotNull(message = "카테코리를 입력해주세요.")
        SkillCategory category
) {
}
