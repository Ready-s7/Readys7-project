package com.example.readys7project.domain.user.auth.service;

import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.dto.request.UpdateUserInformationRequestDto;
import com.example.readys7project.domain.user.auth.dto.response.GetUserInformationResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.UpdateUserInformationResponseDto;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.AdminException;
import com.example.readys7project.global.exception.domain.ClientException;
import com.example.readys7project.global.exception.domain.DeveloperException;
import com.example.readys7project.global.exception.domain.UserException;
import com.example.readys7project.global.security.CustomUserDetails;
import com.example.readys7project.global.security.refreshtoken.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DeveloperRepository developerRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Nested
    @DisplayName("유저 정보 조회 테스트")
    class GetUserInformation {
        @Test
        @DisplayName("성공: 유저 정보 조회")
        void getUserInformation_Success() {
            // given
            User user = User.builder()
                    .email("test@email.com")
                    .name("Tester")
                    .userRole(UserRole.CLIENT)
                    .build();
            ReflectionUtils.setField(user, "id", 1L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            GetUserInformationResponseDto response = userService.getUserInformation(customUserDetails);

            // then
            assertThat(response.email()).isEqualTo(user.getEmail());
            assertThat(response.name()).isEqualTo(user.getName());
        }

        @Test
        @DisplayName("실패: 유저를 찾을 수 없음")
        void getUserInformation_Fail_UserNotFound() {
            // given
            User user = User.builder().build();
            ReflectionUtils.setField(user, "id", 1L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);

            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserInformation(customUserDetails))
                    .isInstanceOf(UserException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("유저 정보 수정 테스트")
    class UpdateUserInformation {
        @Test
        @DisplayName("성공: 유저 정보 수정")
        void updateUserInformation_Success() {
            // given
            User user = User.builder()
                    .email("test@email.com")
                    .name("OldName")
                    .build();
            ReflectionUtils.setField(user, "id", 1L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            UpdateUserInformationRequestDto request = new UpdateUserInformationRequestDto("NewName", "01099998888", "NewDesc");

            given(userRepository.findById(1L)).willReturn(Optional.of(user));

            // when
            UpdateUserInformationResponseDto response = userService.updateUserInformation(customUserDetails, request);

            // then
            assertThat(response.name()).isEqualTo("NewName");
            assertThat(response.phoneNumber()).isEqualTo("01099998888");
            verify(userRepository).saveAndFlush(user);
        }

        @Test
        @DisplayName("실패: 수정하려는 유저를 찾을 수 앖음")
        void updateUserInformation_fail_userNotFound() {
            // given
            User user = User.builder().build();
            ReflectionUtils.setField(user, "id", 1L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            UpdateUserInformationRequestDto requestDto =
                    new UpdateUserInformationRequestDto("NewName", "01099998888", "NewDesc");

            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.updateUserInformation(customUserDetails, requestDto))
                    .isInstanceOf(UserException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 테스트")
    class DeleteUser {
        @Test
        @DisplayName("성공: Client 회원 탈퇴")
        void deleteUser_Client_Success() {
            // given
            User user = User.builder().email("test@email.com").userRole(UserRole.CLIENT).build();
            ReflectionUtils.setField(user, "id", 1L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            Client client = Client.builder().user(user).build();
            ReflectionUtils.setField(client, "id", 10L);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(clientRepository.findByUser(user)).willReturn(Optional.of(client));

            // when
            userService.deleteUser(customUserDetails);

            // then
            verify(clientRepository).deleteById(10L);
            verify(refreshTokenRepository).deleteByEmail(user.getEmail());
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("성공: Developer 회원 탈퇴")
        void deleteUser_Developer_Success() {
            // given
            User user = User.builder().email("dev@email.com").userRole(UserRole.DEVELOPER).build();
            ReflectionUtils.setField(user, "id", 2L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            Developer developer = Developer.builder().user(user).build();
            ReflectionUtils.setField(developer, "id", 20L);

            given(userRepository.findById(2L)).willReturn(Optional.of(user));
            given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));

            // when
            userService.deleteUser(customUserDetails);

            // then
            verify(developerRepository).deleteById(20L);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("성공: Admin 회원 탈퇴")
        void deleteUser_Admin_Success() {
            // given
            User user = User.builder().email("admin@email.com").userRole(UserRole.ADMIN).build();
            ReflectionUtils.setField(user, "id", 3L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            Admin admin = Admin.builder().user(user).build();
            ReflectionUtils.setField(admin, "id", 30L);

            given(userRepository.findById(3L)).willReturn(Optional.of(user));
            given(adminRepository.findByUser(user)).willReturn(Optional.of(admin));

            // when
            userService.deleteUser(customUserDetails);

            // then
            verify(adminRepository).deleteById(30L);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("실패: Client 역할이지만 상세 정보가 없음")
        void deleteUser_fail_clientNotFound() {
            // given
            User user = User.builder().userRole(UserRole.CLIENT).build();
            ReflectionUtils.setField(user, "id", 1L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(clientRepository.findByUser(user)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(customUserDetails))
                    .isInstanceOf(ClientException.class)
                    .hasMessage(ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: Developer 역할이지만 상세정보가 없음")
        void deleteUser_developer_fail_developerNotFound() {
            // given
            User user = User.builder().userRole(UserRole.DEVELOPER).build();
            ReflectionUtils.setField(user, "id", 1L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(developerRepository.findByUser(user)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(customUserDetails))
                    .isInstanceOf(DeveloperException.class)
                    .hasMessage(ErrorCode.DEVELOPER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("실패: Admin 역할이지만 상세정보가 없음")
        void deleteUser_admin_fail_adminNotFound() {
            // given
            User user = User.builder().userRole(UserRole.ADMIN).build();
            ReflectionUtils.setField(user, "id", 1L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);

            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(adminRepository.findByUser(user)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(customUserDetails))
                    .isInstanceOf(AdminException.class)
                    .hasMessage(ErrorCode.ADMIN_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("refreshToken 삭제 테스트")
    class RefreshToken {
        @Test
        @DisplayName("성공: Developer 회원 탈퇴 및 토큰 삭제 확인")
        void deleteUser_developer_success_withToken() {
            // given
            User user = User.builder().email("dev@eamil.com").userRole(UserRole.DEVELOPER).build();
            ReflectionUtils.setField(user, "id", 2L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            Developer developer = Developer.builder().user(user).build();
            ReflectionUtils.setField(developer, "id", 20L);

            given(userRepository.findById(2L)).willReturn(Optional.of(user));
            given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));

            // when
            userService.deleteUser(customUserDetails);

            // then
            verify(developerRepository).deleteById(20L);
            verify(refreshTokenRepository).deleteByEmail("dev@email.com");
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("성공: Admin 회원 탈퇴 및 토큰 삭제 확인")
        void deleteUser_admin_success_withToken() {

            // given
            User user = User.builder().email("admin@eamil.com").userRole(UserRole.ADMIN).build();
            ReflectionUtils.setField(user, "id", 3L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            Admin admin = Admin.builder().user(user).build();
            ReflectionUtils.setField(admin, "id", 30L);

            given(userRepository.findById(3L)).willReturn(Optional.of(user));
            given(adminRepository.findByUser(user)).willReturn(Optional.of(admin));

            // when
            userService.deleteUser(customUserDetails);

            // then
            verify(adminRepository).deleteById(30L);
            verify(refreshTokenRepository).deleteByEmail("admin@eamil.com");
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("성공: Client 회원 탈퇴 및 토큰 삭제 확인")
        void deleteUser_client_success_withToken() {
            // given
            User user = User.builder().email("client@email.com").userRole(UserRole.CLIENT).build();
            ReflectionUtils.setField(user, "id", 4L);
            CustomUserDetails customUserDetails = new CustomUserDetails(user);
            Client client = Client.builder().user(user).build();
            ReflectionUtils.setField(client, "id", 40L);

            given(userRepository.findById(4L)).willReturn(Optional.of(user));
            given(clientRepository.findByUser(user)).willReturn(Optional.of(client));

            // when
            userService.deleteUser(customUserDetails);

            // then
            verify(clientRepository).deleteById(40L);
            verify(refreshTokenRepository).deleteByEmail("client@email.com");
            verify(userRepository).delete(user);
        }
    }
}
