package com.example.readys7project.domain.user.auth.controller;

import com.example.readys7project.domain.user.auth.dto.request.UpdateUserInformationRequestDto;
import com.example.readys7project.domain.user.auth.dto.response.GetUserInformationResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.UpdateUserInformationResponseDto;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.service.UserService;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.common.GlobalExceptionHandler;
import com.example.readys7project.global.exception.domain.UserException;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return new CustomUserDetails(User.builder().email("test@email.com").build());
                    }
                })
                .build();
    }

    @Nested
    @DisplayName("유저 정보 컨트롤러 테스트")
    class UserInfo {
        @Test
        @DisplayName("성공: 내 정보 조회")
        void getUserInformation_Success() throws Exception {
            // given
            GetUserInformationResponseDto response = GetUserInformationResponseDto.builder()
                    .email("test@email.com")
                    .name("Tester")
                    .build();
            given(userService.getUserInformation(any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("test@email.com"))
                    .andExpect(jsonPath("$.data.name").value("Tester"));
        }

        @Test
        @DisplayName("실패: 내 정보 조회 (유저 없음)")
        void getUserInformation_Fail_UserNotFound() throws Exception {
            // given
            given(userService.getUserInformation(any()))
                    .willThrow(new UserException(ErrorCode.USER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/v1/users/me"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("성공: 내 정보 수정")
        void updateUserInformation_Success() throws Exception {
            // given
            UpdateUserInformationRequestDto request = new UpdateUserInformationRequestDto("NewName", "01012345678", "Desc");
            UpdateUserInformationResponseDto response = new UpdateUserInformationResponseDto(1L, "test@email.com", "NewName", "01012345678", "Desc", null);
            given(userService.updateUserInformation(any(), any())).willReturn(response);

            // when & then
            mockMvc.perform(put("/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("NewName"));
        }

        @Test
        @DisplayName("성공: 회원 탈퇴")
        void deleteUser_Success() throws Exception {
            // when & then
            mockMvc.perform(delete("/v1/users/me"))
                    .andExpect(status().isNoContent());
        }
    }
}
