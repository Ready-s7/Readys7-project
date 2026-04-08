package com.example.readys7project.domain.proposal.dto.request;

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
public class ProposalRequest {

    @NotNull(message = "프로젝트 ID는 필수입니다")
    private Long projectId;

    @NotBlank(message = "커버레터는 필수입니다")
    private String coverLetter;

    @NotBlank(message = "제안 예산은 필수입니다")
    private String proposedBudget;

    @NotBlank(message = "제안 기간은 필수입니다")
    private String proposedDuration;
}
