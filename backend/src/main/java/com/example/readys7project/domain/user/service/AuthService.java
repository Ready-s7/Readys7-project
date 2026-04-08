package com.example.readys7project.domain.user.service;

import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.dto.UserDto;
import com.example.readys7project.domain.user.dto.request.LoginRequest;
import com.example.readys7project.domain.user.dto.request.RegisterRequest;
import com.example.readys7project.domain.user.dto.response.AuthResponse;
import com.example.readys7project.domain.user.entity.User;
import com.example.readys7project.domain.user.repository.UserRepository;
import com.example.readys7project.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DeveloperRepository developerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(User.UserRole.valueOf(request.getRole().toUpperCase()))
                .phoneNumber(request.getPhoneNumber())
                .location(request.getLocation())
                .description(request.getDescription())
                .active(true)
                .build();

        user = userRepository.save(user);

        // 개발자로 등록시 Developer 프로필 생성
        if (user.getRole() == User.UserRole.DEVELOPER) {
            Developer developer = Developer.builder()
                    .user(user)
                    .title("개발자")
                    .rating(0.0)
                    .reviewCount(0)
                    .completedProjects(0)
                    .availableForWork(true)
                    .build();
            developerRepository.save(developer);
        }

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities("USER")
                .build();

        String token = tokenProvider.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .user(convertToUserDto(user))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        return AuthResponse.builder()
                .token(token)
                .user(convertToUserDto(user))
                .build();
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
