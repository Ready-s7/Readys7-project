package com.example.readys7project.domain.user.admin.controller;

import com.example.readys7project.domain.user.admin.dto.request.UpdateAdminStatusRequestDto;
import com.example.readys7project.domain.user.admin.dto.response.GetAllAdminListResponseDto;
import com.example.readys7project.domain.user.admin.dto.response.UpdateAdminStatusResponseDto;
import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.service.AdminService;
import com.example.readys7project.global.aop.AdminOnly;
import com.example.readys7project.global.dto.ApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @AdminOnly(role = AdminRole.SUPER_ADMIN)
    @GetMapping("/v1/admins")
    public ResponseEntity<ApiResponseDto<GetAllAdminListResponseDto>> getAllPendingAdminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, adminService.getAllPendingAdminList(pageable)));
    }

    @AdminOnly(role = AdminRole.SUPER_ADMIN)
    @PatchMapping("/v1/admins/{adminId}")
    public ResponseEntity<ApiResponseDto<UpdateAdminStatusResponseDto>> updateAdminStatus(
            @PathVariable Long adminId,
            @RequestBody UpdateAdminStatusRequestDto updateAdminStatusRequestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, adminService.updateAdminStatus(adminId, updateAdminStatusRequestDto)));
    }
}
