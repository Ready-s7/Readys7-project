package com.example.readys7project.domain.user.service;

import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.dto.UserDto;
import com.example.readys7project.domain.user.dto.request.LoginRequest;
import com.example.readys7project.domain.user.dto.request.RegisterRequest;
import com.example.readys7project.domain.user.dto.response.AuthResponse;
import com.example.readys7project.domain.user.entity.User;
import com.example.readys7project.domain.user.enums.UserRole;
import com.example.readys7project.domain.user.repository.UserRepository;
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
    private final DeveloperRepository developerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(UserRole.valueOf(request.getRole().toUpperCase()))
                .phoneNumber(request.getPhoneNumber())
                .location(request.getLocation())
                .description(request.getDescription())
                .build();

        User savedUser = userRepository.save(user);

        // 개발자로 등록시 Developer 프로필 생성
        if (user.getRole() == UserRole.DEVELOPER) {
            Developer developer = Developer.builder()
                    .user(savedUser)
                    .title("개발자")
                    .rating(0.0)
                    .reviewCount(0)
                    .completedProjects(0)
                    .availableForWork(true)
                    .build();
            developerRepository.save(developer);
        }

        return AuthResponse.builder()
                .user(convertToUserDto(user))
                .build();
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
                .role(user.getRole().name())
                .phoneNumber(user.getPhoneNumber())
                .location(user.getLocation())
                .avatarUrl(user.getAvatarUrl())
                .description(user.getDescription())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
