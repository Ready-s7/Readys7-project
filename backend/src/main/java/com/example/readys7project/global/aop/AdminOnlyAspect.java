package com.example.readys7project.global.aop;

import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.admin.repository.AdminRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.AdminException;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminOnlyAspect {

    private final AdminRepository adminRepository;

    @Before("@annotation(adminOnly)")
    public void validateAdmin(AdminOnly adminOnly) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AdminException(ErrorCode.USER_NOT_FOUND);
        }

        User user = userDetails.getUser();

        // 1. 일반 사용자 역할이 ADMIN인지 검증
        if (!user.getUserRole().equals(UserRole.ADMIN)) {
            throw new AdminException(ErrorCode.USER_FORBIDDEN);
        }

        // 2. Admin 엔티티 존재 여부 확인
        Admin admin = adminRepository.findByUser(user).orElseThrow(
                () -> new AdminException(ErrorCode.ADMIN_NOT_FOUND)
        );

        // 3. 관리자 승인 상태 검증
        if (!admin.getStatus().equals(AdminStatus.APPROVED)) {
            throw new AdminException(ErrorCode.ADMIN_NOT_APPROVED);
        }

        // 4. 특정 AdminRole 요구 시 검증 (SUPER_ADMIN은 항상 통과)
        AdminRole requiredRole = adminOnly.role();
        if (admin.getAdminRole() != AdminRole.SUPER_ADMIN && admin.getAdminRole() != requiredRole) {
            throw new AdminException(ErrorCode.INVALID_ADMIN_ROLE);
        }
    }
}
