package com.example.readys7project.domain.user.admin.dto.request;

import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import jakarta.validation.constraints.NotBlank;

public record UpdateAdminStatusRequestDto(

        // adminId는 PathVariable로 받으니, 요청 Dto에서는 제외

        // 상태값만 받아주기
        @NotBlank(message = "상태는 필수 입니다.")
        AdminStatus adminStatus
) {}
