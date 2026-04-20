package com.example.readys7project.domain.user.auth.controller;

import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.auth.dto.request.AdminRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.ClientRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.DeveloperRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.response.AdminRegisterResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.ClientRegisterResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.DeveloperRegisterResponseDto;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.service.AuthService;
import com.example.readys7project.domain.user.enums.ParticipateType;
import com.example.readys7project.global.dto.LoginRequestDto;
import com.example.readys7project.global.dto.LoginResponseDto;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
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
    @DisplayName("회원가입 컨트롤러 테스트")
    class Register {
        @Test
        @DisplayName("성공: Client 회원가입")
        void registerClient_Success() throws Exception {
            // given
            ClientRegisterRequestDto request = new ClientRegisterRequestDto(
                    "client@email.com", "password123", "Tester", "01012345678",
                    "Desc", "Title", ParticipateType.INDIVIDUAL
            );
            ClientRegisterResponseDto response = ClientRegisterResponseDto.builder()
                    .id(1L).email("client@email.com").name("Tester").clientId(1L).title("Title").build();
            given(authService.registerClient(any(ClientRegisterRequestDto.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/v1/auth/register/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.email").value("client@email.com"))
                    .andExpect(jsonPath("$.data.name").value("Tester"));
        }

        @Test
        @DisplayName("실패: Client 회원가입 (이메일 공백)")
        void registerClient_Fail_EmptyEmail() throws Exception {
            // given
            ClientRegisterRequestDto request = new ClientRegisterRequestDto(
                    "", "password123", "Tester", "01012345678",
                    "Desc", "Title", ParticipateType.INDIVIDUAL
            );

            // when & then
            mockMvc.perform(post("/v1/auth/register/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("성공: Developer 회원가입")
        void registerDeveloper_Success() throws Exception {
            // given
            DeveloperRegisterRequestDto request = new DeveloperRegisterRequestDto(
                    "dev@email.com", "password123", "Developer", "01012345678", "title", 10000, 50000, List.of("Java"), "1시간", true, ParticipateType.INDIVIDUAL
            );
            DeveloperRegisterResponseDto response = DeveloperRegisterResponseDto.builder().email("dev@email.com").build();
            given(authService.registerDeveloper(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/v1/auth/register/developers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("성공: Admin 회원가입")
        void registerAdmin_Success() throws Exception {
            // given
            AdminRegisterRequestDto request = new AdminRegisterRequestDto("admin@email.com", "password123", "AdminUser", "01012345678", AdminRole.SUPER_ADMIN);
            AdminRegisterResponseDto response = AdminRegisterResponseDto.builder().email("admin@email.com").build();
            given(authService.registerAdmin(any())).willReturn(response);

            // when & then
            mockMvc.perform(post("/v1/auth/register/admins")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("로그인 컨트롤러 테스트")
    class Login {
        @Test
        @DisplayName("성공: 로그인")
        void login_Success() throws Exception {
            // given
            LoginRequestDto request = new LoginRequestDto("test@email.com", "password");
            AuthService.AuthTokenDto tokens = new AuthService.AuthTokenDto("accessToken", "refreshToken", "test@email.com");
            given(authService.login(any(LoginRequestDto.class))).willReturn(tokens);

            // when & then
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Authorization", "Bearer accessToken"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"))
                    .andExpect(jsonPath("$.data.email").value("test@email.com"));
        }

        @Test
        @DisplayName("실패: 로그인 (잘못된 비밀번호)")
        void login_Fail_InvalidPassword() throws Exception {
            // given
            LoginRequestDto request = new LoginRequestDto("test@email.com", "wrongPassword");
            given(authService.login(any(LoginRequestDto.class)))
                    .willThrow(new UserException(ErrorCode.USER_INFO_MISMATCH));

            // when & then
            mockMvc.perform(post("/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 컨트롤러 테스트")
    class Reissue {
        @Test
        @DisplayName("성공: 토큰 재발급")
        void reissue_Success() throws Exception {
            // given
            LoginResponseDto request = LoginResponseDto.builder().refreshToken("oldRefreshToken").build();
            AuthService.AuthTokenDto tokens = new AuthService.AuthTokenDto("newAccessToken", "newRefreshToken", "test@email.com");
            given(authService.reissue("oldRefreshToken")).willReturn(tokens);

            // when & then
            mockMvc.perform(post("/v1/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Authorization", "Bearer newAccessToken"))
                    .andExpect(jsonPath("$.data.refreshToken").value("newRefreshToken"));
        }
    }

    @Nested
    @DisplayName("로그아웃 컨트롤러 테스트")
    class Logout {
        @Test
        @DisplayName("성공: 로그아웃")
        void logout_Success() throws Exception {
            // when & then
            mockMvc.perform(post("/v1/auth/logout"))
                    .andExpect(status().isNoContent());
        }
    }
}
