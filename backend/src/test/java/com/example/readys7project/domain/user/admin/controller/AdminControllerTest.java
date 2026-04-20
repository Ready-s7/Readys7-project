package com.example.readys7project.domain.user.admin.controller;

import com.example.readys7project.domain.user.admin.dto.request.UpdateAdminStatusRequestDto;
import com.example.readys7project.domain.user.admin.dto.response.AdminSummaryResponseDto;
import com.example.readys7project.domain.user.admin.dto.response.GetAllAdminListResponseDto;
import com.example.readys7project.domain.user.admin.dto.response.UpdateAdminStatusResponseDto;
import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.admin.service.AdminService;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private AdminController adminController;

    @Mock
    private AdminService adminService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        User user = User.builder().email("admin@email.com").build();
                        ReflectionTestUtils.setField(user, "id", 1L);
                        return new CustomUserDetails(user);
                    }
                })
                .build();
    }

    @Nested
    @DisplayName("관리자 대기 목록 조회 API 테스트")
    class GetAllPendingAdminList {
        @Test
        @DisplayName("성공: 관리자 대기 목록 조회")
        void getAllPendingAdminList_Success() throws Exception {
            // given
            AdminSummaryResponseDto summary = new AdminSummaryResponseDto(
                    1L, 1L, "admin1@email.com", "Admin1", AdminRole.SUPER_ADMIN, AdminStatus.PENDING, LocalDateTime.now()
            );
            GetAllAdminListResponseDto response = new GetAllAdminListResponseDto(
                    List.of(summary), 1, 10, 1L, 1
            );
            given(adminService.getAllPendingAdminList(any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/v1/admins")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.admins[0].adminId").value(1))
                    .andExpect(jsonPath("$.data.admins[0].name").value("Admin1"));
        }
    }

    @Nested
    @DisplayName("관리자 상태 변경 API 테스트")
    class UpdateAdminStatus {
        @Test
        @DisplayName("성공: 관리자 상태 변경")
        void updateAdminStatus_Success() throws Exception {
            // given
            Long adminId = 1L;
            UpdateAdminStatusRequestDto request = new UpdateAdminStatusRequestDto(AdminStatus.APPROVED);
            UpdateAdminStatusResponseDto response = new UpdateAdminStatusResponseDto(
                    adminId, "Admin1", AdminRole.SUPER_ADMIN, AdminStatus.APPROVED, LocalDateTime.now()
            );
            given(adminService.updateAdminStatus(eq(adminId), any())).willReturn(response);

            // when & then
            mockMvc.perform(patch("/v1/admins/{adminId}", adminId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.adminStatus").value("APPROVED"));
        }
    }
}
