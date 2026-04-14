package com.example.readys7project.domain.review.dto.request;


import jakarta.validation.constraints.*;
import lombok.Builder;


@Builder
public record ReviewUpdateRequestDto(


    @Min(value = 1, message = "평점은 1점 이상이어야 합니다")
    @Max(value = 5, message = "평점은 5점 이하여야 합니다")
    Integer rating,

     String comment
){}
