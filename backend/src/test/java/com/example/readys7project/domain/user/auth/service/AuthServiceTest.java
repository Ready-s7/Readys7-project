package com.example.readys7project.domain.user.auth.service;

import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.dto.request.AdminRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.ClientRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.DeveloperRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.response.AdminRegisterResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.ClientRegisterResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.DeveloperRegisterResponseDto;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.enums.ParticipateType;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.dto.LoginRequestDto;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.AdminException;
import com.example.readys7project.global.exception.domain.UserException;
import com.example.readys7project.global.security.JwtTokenProvider;
import com.example.readys7project.global.security.refreshtoken.entity.RefreshToken;
import com.example.readys7project.global.security.refreshtoken.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private DeveloperRepository developerRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AdminRepository adminRepository;

    @Nested
    @DisplayName("Client 회원가입 테스트")
    class RegisterClient {
        @Test
        @DisplayName("성공: Client 회원가입")
        void registerClient_Success() {
            // given
            ClientRegisterRequestDto request = new ClientRegisterRequestDto(
                    "test@email.com", "password123", "Tester", "01012345678",
                    "Description", "Title", ParticipateType.INDIVIDUAL
            );
            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

            User user = User.builder()
                    .email(request.email())
                    .name(request.name())
                    .userRole(UserRole.CLIENT)
                    .build();
            ReflectionUtils.setField(user, "id", 1L);
            given(userRepository.save(any(User.class))).willReturn(user);

            Client client = Client.builder()
                    .user(user)
                    .title(request.title())
                    .build();
            ReflectionUtils.setField(client, "id", 1L);
            given(clientRepository.save(any(Client.class))).willReturn(client);

            // when
            ClientRegisterResponseDto response = authService.registerClient(request);

            // then
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.name()).isEqualTo(request.name());
            assertThat(response.clientId()).isEqualTo(1L);
            verify(userRepository).save(any(User.class));
            verify(clientRepository).save(any(Client.class));
        }

        @Test
        @DisplayName("실패: 중복된 이메일")
        void registerClient_Fail_DuplicateEmail() {
            // given
            ClientRegisterRequestDto request = new ClientRegisterRequestDto(
                    "duplicate@email.com", "password", "name", "01012345678", "desc", "title", ParticipateType.INDIVIDUAL
            );
            given(userRepository.existsByEmail(request.email())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.registerClient(request))
                    .isInstanceOf(UserException.class)
                    .hasMessage(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("Developer 회원가입 테스트")
    class RegisterDeveloper {
        @Test
        @DisplayName("성공: Developer 회원가입")
        void registerDeveloper_Success() {
            // given
            DeveloperRegisterRequestDto request = new DeveloperRegisterRequestDto(
                    "dev@email.com", "password123", "Developer", "01012345678",
                    "DevTitle", 10000, 50000, List.of("Java", "Spring"), "1시간", true, ParticipateType.INDIVIDUAL
            );
            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");

            User user = User.builder()
                    .email(request.email())
                    .name(request.name())
                    .userRole(UserRole.DEVELOPER)
                    .build();
            ReflectionUtils.setField(user, "id", 2L);
            given(userRepository.save(any(User.class))).willReturn(user);

            Developer developer = Developer.builder()
                    .user(user)
                    .title(request.title())
                    .build();
            ReflectionUtils.setField(developer, "id", 1L);
            given(developerRepository.save(any(Developer.class))).willReturn(developer);

            // when
            DeveloperRegisterResponseDto response = authService.registerDeveloper(request);

            // then
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.developerId()).isEqualTo(1L);
            verify(userRepository).save(any(User.class));
            verify(developerRepository).save(any(Developer.class));
        }
    }


    @Nested
    @DisplayName("Admin 회원가입 테스트")
    class RegisterAdmin {
        @Test
        @DisplayName("성공: Admin 회원가입")
        void registerAdmin_Success() {
            // given
            AdminRegisterRequestDto request = new AdminRegisterRequestDto(
                    "admin@email.com", "password", "Admin", "010", AdminRole.SUPER_ADMIN
            );
            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn("encoded");

            User user = User.builder().email(request.email()).userRole(UserRole.ADMIN).build();
            ReflectionUtils.setField(user, "id", 3L);
            given(userRepository.save(any(User.class))).willReturn(user);

            Admin admin = Admin.builder().user(user).adminRole(AdminRole.SUPER_ADMIN).build();
            ReflectionUtils.setField(admin, "id", 1L);
            given(adminRepository.save(any(Admin.class))).willReturn(admin);

            // when
            AdminRegisterResponseDto response = authService.registerAdmin(request);

            // then
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.adminRole()).isEqualTo(AdminRole.SUPER_ADMIN);
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class Login {
        @Test
        @DisplayName("성공: 로그인")
        void login_Success() {
            // given
            LoginRequestDto request = new LoginRequestDto("test@email.com", "password123");
            User user = User.builder()
                    .email(request.email())
                    .password("encodedPassword")
                    .userRole(UserRole.CLIENT)
                    .build();
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
            given(jwtTokenProvider.createToken(user.getEmail())).willReturn("accessToken");
            given(jwtTokenProvider.createRefreshToken(user.getEmail())).willReturn("refreshToken");
            given(refreshTokenRepository.existsByEmail(user.getEmail())).willReturn(false);

            // when
            AuthService.AuthTokenDto result = authService.login(request);

            // then
            assertThat(result.accessToken()).isEqualTo("accessToken");
            assertThat(result.refreshToken()).isEqualTo("refreshToken");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("실패: 비밀번호 불일치")
        void login_Fail_PasswordMismatch() {
            // given
            LoginRequestDto request = new LoginRequestDto("test@email.com", "wrongPassword");
            User user = User.builder().email(request.email()).password("encodedPassword").build();
            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UserException.class)
                    .hasMessage(ErrorCode.USER_INFO_MISMATCH.getMessage());
        }

        @Test
        @DisplayName("실패: 어드민 미승인 상태")
        void login_Fail_AdminNotApproved() {
            // given
            LoginRequestDto request = new LoginRequestDto("admin@email.com", "password");
            User user = User.builder().email(request.email()).password("encoded").userRole(UserRole.ADMIN).build();
            Admin admin = Admin.builder().user(user).status(AdminStatus.PENDING).build();

            given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
            given(adminRepository.findByUser(user)).willReturn(Optional.of(admin));

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AdminException.class)
                    .hasMessage(ErrorCode.ADMIN_NOT_APPROVED.getMessage());
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class Reissue {
        @Test
        @DisplayName("성공: 토큰 재발급")
        void reissue_Success() {
            // given
            String refreshToken = "validRefreshToken";
            RefreshToken savedToken = new RefreshToken("test@email.com", refreshToken, LocalDateTime.now().plusDays(1));
            User user = User.builder().email("test@email.com").build();

            given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.of(savedToken));
            given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(user));
            given(jwtTokenProvider.createRefreshToken(user.getEmail())).willReturn("newRefreshToken");
            given(jwtTokenProvider.createToken(user.getEmail())).willReturn("newAccessToken");

            // when
            AuthService.AuthTokenDto result = authService.reissue(refreshToken);

            // then
            assertThat(result.accessToken()).isEqualTo("newAccessToken");
            assertThat(result.refreshToken()).isEqualTo("newRefreshToken");
            verify(jwtTokenProvider).validateToken(refreshToken);
        }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class Logout {
        @Test
        @DisplayName("성공: 로그아웃")
        void logout_Success() {
            // given
            String email = "test@email.com";

            // when
            authService.logout(email);

            // then
            verify(refreshTokenRepository).deleteByEmail(email);
        }
    }
}

// Utility class to set private fields like 'id' using reflection
class ReflectionUtils {
    public static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            // For fields inherited from parent classes (like id in BaseEntity)
            try {
                java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
