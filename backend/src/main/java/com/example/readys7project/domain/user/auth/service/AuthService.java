package com.example.readys7project.domain.user.auth.service;

import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.dto.UserDto;
import com.example.readys7project.domain.user.auth.dto.request.AdminRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.ClientRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.DeveloperRegisterRequestDto;
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

    // Client 회원가입 로직
    @Transactional
    public UserDto registerClient(
            ClientRegisterRequestDto clientRegisterRequestDto
    ) {
        // 해당 이메일이 이미 존재하는지 확인
        if (userRepository.existsByEmail(clientRegisterRequestDto.email())) {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 유저 객체 생성
        User user = User.builder()
                .email(clientRegisterRequestDto.email())
                .password(passwordEncoder.encode(clientRegisterRequestDto.password()))
                .name(clientRegisterRequestDto.name())
                .userRole(UserRole.CLIENT)
                .phoneNumber(clientRegisterRequestDto.phoneNumber())
                .description(clientRegisterRequestDto.description())
                .build();

        // 유저 레포에 저장
        User savedUser = userRepository.save(user);

        // 클라이언트 레포에 저장
        clientRepository.save(Client.builder()
                .user(savedUser)
                .title(clientRegisterRequestDto.title())
                .participateType(clientRegisterRequestDto.participateType())
                .rating(0.0)
                .reviewCount(0)
                .completedProject(0)
                .build());

        // Dto 반환
        return convertToUserDto(savedUser);
    }

    @Transactional
    public UserDto registerDeveloper(DeveloperRegisterRequestDto developerRegisterRequestDto) {

        // 해당 이메일이 이미 존재하는지 확인
        if (userRepository.existsByEmail(developerRegisterRequestDto.email())) {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 유저 생성
        User user = User.builder()
                .email(developerRegisterRequestDto.email())
                .password(passwordEncoder.encode(developerRegisterRequestDto.password()))
                .name(developerRegisterRequestDto.name())
                .userRole(UserRole.DEVELOPER)
                .phoneNumber(developerRegisterRequestDto.phoneNumber())
                .build();

        // 유저 레포에 저장
        User savedUser = userRepository.save(user);

        // 개발자 레포에 저장
        developerRepository.save(Developer.builder()
                .user(savedUser)
                .title(developerRegisterRequestDto.title())
                .minHourlyPay(developerRegisterRequestDto.minHourlyPay())
                .maxHourlyPay(developerRegisterRequestDto.maxHourlyPay())
                .skills(developerRegisterRequestDto.skills())
                .availableForWork(developerRegisterRequestDto.availableForWork())
                        .rating(0.0)
                        .reviewCount(0)
                        .completedProjects(0)
                .build());

        // Dto 반환
        return convertToUserDto(savedUser);
    }

    @Transactional
    public UserDto registerAdmin(AdminRegisterRequestDto adminRegisterRequestDto) {

        // 해당 이메일이 이미 존재하는지 확인
        if (userRepository.existsByEmail(adminRegisterRequestDto.email())) {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 유저 생성
        User user = User.builder()
                .email(adminRegisterRequestDto.email())
                .password(passwordEncoder.encode(adminRegisterRequestDto.password()))
                .name(adminRegisterRequestDto.name())
                .userRole(UserRole.ADMIN)
                .phoneNumber(adminRegisterRequestDto.phoneNumber())
                .build();

        // 유저 레포에 저장
        User savedUser = userRepository.save(user);

        // 어드민 레포에 저장
        adminRepository.save(Admin.builder()
                .user(savedUser)
                .adminRole(adminRegisterRequestDto.adminRole())
                .build());

        // Dto 반환
        return convertToUserDto(savedUser);
    }


    // Access Token + Refresh Token + email을 묶어서 반환하는 record
    public record AuthTokenDto(String accessToken, String refreshToken, String email) {
    }

    @Transactional
    public AuthTokenDto login(LoginRequestDto request) {
        // email로 User 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserException(ErrorCode.USER_INFO_MISMATCH);
        }

        // Access Token 발급
        String accessToken = jwtTokenProvider.createToken(
                user.getEmail()
        );

        // 기존 토큰 삭제 추가
        boolean existence = refreshTokenRepository.existsByEmail(user.getEmail());
        if (existence) {
            refreshTokenRepository.deleteByEmail(user.getEmail());
            refreshTokenRepository.flush();
        }

        // Refresh Token 발급 및 저장 (Rotation)
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
        // Refresh Token 유효성 검증
        jwtTokenProvider.validateToken(refreshToken);

        // DB에서 Refresh Token 조회
        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UserException(ErrorCode.USER_UNAUTHORIZED));

        // email로 User 조회
        User user = userRepository.findByEmail(savedToken.getEmail())
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // Refresh Token Rotation
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        savedToken.rotate(newRefreshToken, jwtTokenProvider.getRefreshTokenExpiresAt());
        refreshTokenRepository.save(savedToken);

        // 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.createToken(
                user.getEmail()
        );

        return new AuthTokenDto(newAccessToken, newRefreshToken, user.getEmail());
    }

    // 로그아웃
    public void logout(String email) {
        // 토큰 삭제 -> 로그아웃
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
