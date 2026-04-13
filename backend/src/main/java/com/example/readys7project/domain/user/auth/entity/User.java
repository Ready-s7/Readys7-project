package com.example.readys7project.domain.user.auth.entity;

import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserRole userRole; // CLIENT, DEVELOPER, ADMIN

    @Column(nullable = false)
    private String phoneNumber;

    private String description;

    private boolean isDeleted = false;

    @Builder
    public User(String email, String password, String name, UserRole userRole, String phoneNumber, String description) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.userRole = userRole;
        this.phoneNumber = phoneNumber;
        this.description = description;
    }
}
