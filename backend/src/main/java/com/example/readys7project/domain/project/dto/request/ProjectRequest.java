package com.example.readys7project.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ProjectRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    private String description;

    @NotBlank(message = "카테고리는 필수입니다")
    private String category;

    private String budget;
    private String duration;
    private List<String> skills;
}
