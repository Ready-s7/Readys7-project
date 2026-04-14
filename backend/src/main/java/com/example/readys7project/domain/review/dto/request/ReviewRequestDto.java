package com.example.readys7project.domain.review.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Builder
public record ReviewRequestDto (

    @NotNull(message = "프로젝트 번호는 필수 입니다.")
     Long projectId,

    @NotNull(message = "평점은 필수입니다")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
     Integer rating,

    @NotBlank(message = "코멘트는 필수입니다")
    @Size(max = 100, message = "코멘트는 100자 이하여야 합니다.")
     String comment
)
{ }
