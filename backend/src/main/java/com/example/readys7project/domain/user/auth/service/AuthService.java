package com.example.readys7project.domain.user.auth.service;

import com.example.readys7project.domain.user.auth.dto.response.AdminRegisterResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.ClientRegisterResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.DeveloperRegisterResponseDto;
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
    public ClientRegisterResponseDto registerClient(
            ClientRegisterRequestDto clientRegisterRequestDto
    ) {
        // 해당 이메일이 이미 존재하는지 확인
        validateDuplicateEmail(clientRegisterRequestDto.email());

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
        Client savedClient = clientRepository.save(Client.builder()
                .user(savedUser)
                .title(clientRegisterRequestDto.title())
                .participateType(clientRegisterRequestDto.participateType())
                .rating(0.0)
                .reviewCount(0)
                .completedProject(0)
                .build());

        // Dto 반환
        return ClientRegisterResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .clientId(savedClient.getId())
                .title(savedClient.getTitle())
                .build();

    }

    // 개발자 회원 가입
    @Transactional
    public DeveloperRegisterResponseDto registerDeveloper(DeveloperRegisterRequestDto developerRegisterRequestDto) {

        // 해당 이메일이 이미 존재하는지 확인
        validateDuplicateEmail(developerRegisterRequestDto.email());

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
       Developer savedDeveloper = developerRepository.save(Developer.builder()
                .user(savedUser)
                .title(developerRegisterRequestDto.title())
                .minHourlyPay(developerRegisterRequestDto.minHourlyPay())
                .maxHourlyPay(developerRegisterRequestDto.maxHourlyPay())
                .skills(developerRegisterRequestDto.skills())
                .responseTime(developerRegisterRequestDto.responseTime())
                .availableForWork(developerRegisterRequestDto.availableForWork())
                .rating(0.0)
                .reviewCount(0)
                .completedProjects(0)
                .build());

        // Dto 반환
        return DeveloperRegisterResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .developerId(savedDeveloper.getId())
                .title(savedDeveloper.getTitle())
                .build();
    }


    // 관리자 회원 가입
    @Transactional
    public AdminRegisterResponseDto registerAdmin(AdminRegisterRequestDto adminRegisterRequestDto) {

        // 해당 이메일이 이미 존재하는지 확인
        validateDuplicateEmail(adminRegisterRequestDto.email());

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
        Admin savedAdmin = adminRepository.save(Admin.builder()
                .user(savedUser)
                .adminRole(adminRegisterRequestDto.adminRole())
                .build());

        // Dto 반환
        return AdminRegisterResponseDto.builder()
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .adminId(savedAdmin.getId())
                .adminRole(savedAdmin.getAdminRole())
                .build();
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
    @Transactional
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

    // 이메일이 이미 존재하는지 검증
    private void validateDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }
}
