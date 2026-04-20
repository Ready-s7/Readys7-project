package com.example.readys7project.domain.user.admin.service;

import com.example.readys7project.domain.user.admin.dto.request.UpdateAdminStatusRequestDto;
import com.example.readys7project.domain.user.admin.dto.response.GetAllAdminListResponseDto;
import com.example.readys7project.domain.user.admin.dto.response.UpdateAdminStatusResponseDto;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.AdminException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private AdminRepository adminRepository;

    private User createAdminUser(String name) {
        User user = User.builder().name(name).email(name + "@test.com").build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private Admin createAdmin(User user, AdminStatus status) {
        Admin admin = Admin.superAdminBuiler()
                .user(user)
                .adminRole(AdminRole.SUPER_ADMIN)
                .status(status)
                .build();
        // ID 및 상태, 업데이트 시간 강제 설정
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "status", status);
        ReflectionTestUtils.setField(admin, "updatedAt", LocalDateTime.now());
        return admin;
    }

    @Nested
    @DisplayName("관리자 대기 목록 조회 테스트")
    class GetAllPendingAdminList {
        @Test
        @DisplayName("성공: 관리자 대기 목록 조회")
        void getAllPendingAdminList_Success() {
            // given
            Pageable pageable = PageRequest.of(1, 10);
            User user = createAdminUser("Admin1");
            Admin admin = createAdmin(user, AdminStatus.PENDING);

            Page<Admin> adminPage = new PageImpl<>(List.of(admin), PageRequest.of(0, 10), 1);
            given(adminRepository.findAllByStatus(any(AdminStatus.class), any(Pageable.class))).willReturn(adminPage);

            // when
            GetAllAdminListResponseDto response = adminService.getAllPendingAdminList(pageable);

            // then
            assertThat(response.admins()).hasSize(1);
            assertThat(response.admins().get(0).adminId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("관리자 상태 변경 테스트")
    class UpdateAdminStatus {
        @Test
        @DisplayName("성공: 관리자 승인")
        void updateAdminStatus_Approve_Success() {
            // given
            Long adminId = 1L;
            UpdateAdminStatusRequestDto request = new UpdateAdminStatusRequestDto(AdminStatus.APPROVED);
            User user = createAdminUser("Admin1");
            Admin admin = createAdmin(user, AdminStatus.PENDING);

            given(adminRepository.findById(anyLong())).willReturn(Optional.of(admin));

            // when
            UpdateAdminStatusResponseDto response = adminService.updateAdminStatus(adminId, request);

            // then
            assertThat(response.id()).isEqualTo(adminId);
            assertThat(response.adminStatus()).isEqualTo(AdminStatus.APPROVED);
            verify(adminRepository).saveAndFlush(admin);
        }

        @Test
        @DisplayName("성공: 관리자 거절")
        void updateAdminStatus_Reject_Success() {
            // given
            Long adminId = 1L;
            UpdateAdminStatusRequestDto request = new UpdateAdminStatusRequestDto(AdminStatus.REJECTED);
            User user = createAdminUser("Admin1");
            Admin admin = createAdmin(user, AdminStatus.PENDING);

            given(adminRepository.findById(anyLong())).willReturn(Optional.of(admin));

            // when
            UpdateAdminStatusResponseDto response = adminService.updateAdminStatus(adminId, request);

            // then
            assertThat(response.id()).isEqualTo(adminId);
            assertThat(response.adminStatus()).isEqualTo(AdminStatus.REJECTED);
            verify(adminRepository).saveAndFlush(admin);
        }

        @Test
        @DisplayName("실패: 관리자를 찾을 수 없음")
        void updateAdminStatus_Fail_AdminNotFound() {
            // given
            Long adminId = 99L;
            UpdateAdminStatusRequestDto request = new UpdateAdminStatusRequestDto(AdminStatus.APPROVED);
            given(adminRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> adminService.updateAdminStatus(adminId, request))
                    .isInstanceOf(AdminException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADMIN_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 대기 중 상태가 아님")
        void updateAdminStatus_Fail_StatusNotMatch() {
            // given
            Long adminId = 1L;
            UpdateAdminStatusRequestDto request = new UpdateAdminStatusRequestDto(AdminStatus.APPROVED);
            User user = createAdminUser("Admin1");
            Admin admin = createAdmin(user, AdminStatus.APPROVED); // 이미 승인된 상태로 설정

            given(adminRepository.findById(anyLong())).willReturn(Optional.of(admin));

            // when & then
            assertThatThrownBy(() -> adminService.updateAdminStatus(adminId, request))
                    .isInstanceOf(AdminException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADMIN_STATUS_NOT_MATCH);
        }
    }
}
