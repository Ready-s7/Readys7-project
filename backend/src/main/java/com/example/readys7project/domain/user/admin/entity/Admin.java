package com.example.readys7project.domain.user.admin.entity;

import com.example.readys7project.domain.user.admin.enums.AdminRole;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.auth.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {
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


    @Builder
    public Admin(User user, AdminRole adminRole) {
        this.user = user;
        this.adminRole = adminRole;
        this.status = AdminStatus.PENDING;
    }

    public void updateAdminRole(AdminRole adminRole) {
        this.adminRole = adminRole;
    }

    public void updateAdminStatus(AdminStatus status) {
        this.status = status;
    }
}
