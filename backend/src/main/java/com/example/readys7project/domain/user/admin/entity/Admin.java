package com.example.readys7project.domain.user.admin.entity;

import com.example.readys7project.domain.user.admin.dto.request.UpdateAdminStatusRequestDto;
import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.entity.BaseEntity;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.AdminException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;

@Getter
@Entity
@Table(name = "admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SoftDelete

public class Admin extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "admin_role", nullable = false)
    private AdminRole adminRole;

    @Column(name = "status", nullable = false)
    private AdminStatus status;



    // 관리자는 회원가입시 PENDING상태로 강제
    @Builder
    public Admin(User user, AdminRole adminRole) {
        this.user = user;
        this.adminRole = adminRole;
        this.status = AdminStatus.PENDING;
    }

    // 역할 변경
    public void updateAdminRole(AdminRole adminRole) {
        this.adminRole = adminRole;
    }


    // 슈퍼관리자가 승인해주는 메서드

    public void updateAdminStatus(AdminStatus adminStatus) {
        if (this.status != AdminStatus.PENDING) {
            throw new AdminException(ErrorCode.ADMIN_STATUS_NOT_MATCH);
        }
        this.status = adminStatus;
    }

    // builderMethodName -> 각 빌더에 고유한 이름을 지정해줄 수 있는 메서드
    // @Builder 어노테이션을 여러 생성자에 붙이면 빌더 메서드 이름이 충돌할 수 있는데,
    // 이것을 방지하기 위해서 슈퍼어드민 전용 빌더 이름을 지정
    @Builder(builderMethodName = "superAdminBuiler")
    public Admin(User user, AdminRole adminRole, AdminStatus adminStatus) {
        this.user = user;
        this.adminRole = adminRole;
        this.status = adminStatus;
    }

    // InitData에서 사용할 슈퍼 어드민 정적 팩토리 메서드

    public static Admin createSuperAdmin(User user) {
        return Admin.superAdminBuiler()
                .user(user)
                .adminRole(AdminRole.SUPER_ADMIN)
                .adminStatus(AdminStatus.APPROVED)
                .build();

    }
}
