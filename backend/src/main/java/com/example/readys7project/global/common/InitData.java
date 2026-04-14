package com.example.readys7project.global.common;

import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor

public class InitData implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if(adminRepository.findByUserEmail("superAdmin@system.com").isEmpty()) {

            User user = User.builder()
                    .email("superAdmin@system.com")
                    .password(passwordEncoder.encode("12345678"))
                    .name("슈퍼 어드민")
                    .userRole(UserRole.ADMIN)
                    .phoneNumber("010-1212-1212")
                    .description("내가 짱이다.")
                    .build();

            User savedUser = userRepository.save(user);

            Admin admin = Admin.createSuperAdmin(savedUser);

            adminRepository.save(admin);

        }


    }
}
