package com.example.readys7project.domain.user.auth.service;

import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.dto.UserDto;
import com.example.readys7project.domain.user.auth.dto.request.AdminRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.ClientRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.DeveloperRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.UserRegisterRequestDto;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.global.dto.LoginRequestDto;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.UserException;
import com.example.readys7project.global.security.JwtTokenProvider;
import com.example.readys7project.global.security.refreshtoken.entity.RefreshToken;
import com.example.readys7project.global.security.refreshtoken.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeveloperRepository developerRepository;
    private final ClientRepository clientRepository;
    private final AdminRepository adminRepository;

    @Transactional
    public UserDto register(
            UserRegisterRequestDto userRegisterRequestDto,
            AdminRegisterRequestDto adminRegisterRequestDto,
            ClientRegisterRequestDto clientRegisterRequestDto,
            DeveloperRegisterRequestDto developerRegisterRequestDto
    ) {
        if (userRepository.existsByEmail(userRegisterRequestDto.email())) {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(userRegisterRequestDto.email())
                .password(passwordEncoder.encode(userRegisterRequestDto.password()))
                .name(userRegisterRequestDto.name())
                .userRole(UserRole.valueOf(userRegisterRequestDto.role().toUpperCase()))
                .phoneNumber(userRegisterRequestDto.phoneNumber())
                .description(userRegisterRequestDto.description())
                .build();

        User savedUser = userRepository.save(user);


        if (user.getUserRole() == UserRole.ADMIN) {
            Admin admin = Admin.builder()
                    .user(savedUser)
                    .adminRole(adminRegisterRequestDto.adminRole())
                    .build();
            adminRepository.save(admin);
        }

//         개발자로 등록시 Developer 프로필 생성
        if (user.getUserRole() == UserRole.DEVELOPER) {
            Developer developer = Developer.builder()
                    .user(savedUser)
                    .title(developerRegisterRequestDto.title())
                    // TODO minHourlyPay
                    // TODO maxHourlyPay
                    .availableForWork(developerRegisterRequestDto.availableForWork())
                    .build();
            developerRepository.save(developer);
        }
        if (user.getUserRole() == UserRole.CLIENT) {
            Client client = Client.builder()
                    .user(savedUser)
                    .title(clientRegisterRequestDto.title())
                    .participateType(clientRegisterRequestDto.participateType())
                    .build();
            clientRepository.save(client);
        }

        return convertToUserDto(savedUser);
    }

    // Access Token + Refresh Token + email을 묶어서 반환하는 record
    public record AuthTokenDto(String accessToken, String refreshToken, String email) {}

    @Transactional
    public AuthTokenDto login(LoginRequestDto request) {
        // 1. email로 User 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserException(ErrorCode.USER_INFO_MISMATCH);
        }


        // 4. Access Token 발급
        String accessToken = jwtTokenProvider.createToken(
                user.getEmail()
        );

        // 기존 토큰 삭제 추가
        boolean existence = refreshTokenRepository.existsByEmail(user.getEmail());
        if (existence) {
            refreshTokenRepository.deleteByEmail(user.getEmail());
            refreshTokenRepository.flush();
        }

        // 5. Refresh Token 발급 및 저장 (Rotation)
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());


        refreshTokenRepository.save(
                new RefreshToken(
                        user.getEmail(),
                        refreshToken,
                        jwtTokenProvider.getRefreshTokenExpiresAt()
                )
        );

        return new AuthTokenDto(accessToken, refreshToken, user.getEmail());
    }

    @Transactional
    public AuthTokenDto reissue(String refreshToken) {
        // 1. Refresh Token 유효성 검증
        jwtTokenProvider.validateToken(refreshToken);

        // 2. DB에서 Refresh Token 조회
        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UserException(ErrorCode.USER_UNAUTHORIZED));

        // 3. email로 User 조회
        User user = userRepository.findByEmail(savedToken.getEmail())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 5. Refresh Token Rotation
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        savedToken.rotate(newRefreshToken, jwtTokenProvider.getRefreshTokenExpiresAt());
        refreshTokenRepository.save(savedToken);

        // 6. 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.createToken(
                user.getEmail()
        );

        return new AuthTokenDto(newAccessToken, newRefreshToken, user.getEmail());
    }

    public void logout(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }

    private UserDto convertToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getUserRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
