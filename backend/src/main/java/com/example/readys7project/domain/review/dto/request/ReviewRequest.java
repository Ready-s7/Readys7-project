package com.example.readys7project.domain.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "개발자 ID는 필수입니다")
    private Long developerId;

    @NotNull(message = "프로젝트 ID는 필수입니다")
    private Long projectId;

    @NotNull(message = "평점은 필수입니다")
    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
    private Integer rating;

    @NotBlank(message = "코멘트는 필수입니다")
    private String comment;
}
