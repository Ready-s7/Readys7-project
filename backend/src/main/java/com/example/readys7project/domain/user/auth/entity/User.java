package com.example.readys7project.domain.user.auth.entity;

import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserRole userRole; // CLIENT, DEVELOPER, ADMIN

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "description")
    private String description;

    @Column(name = "is_deleted")
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

    // 유저 정보 수정 메서드
    public void updateUserInformation(String name, String phoneNumber, String description) {

        if (name != null && !name.isBlank()) this.name = name;

        if (phoneNumber != null && !phoneNumber.isBlank()) this.phoneNumber = phoneNumber;

        if (description != null && !description.isBlank()) this.description = description;
    }

        /* @NotEmpty -> null은 막는데, "" 빈 문자열은 허용
         @NotEmpty -> null은 막는데, "" 빈 문자열은 허용
         null, "" 빈문자열 다 허용*/

    /* null -> 값 자체가 존재하지 않음
       "" -> 값은 존재하는데 길이가 0인 문자열
      " " -> 공백 문자가 포함된 문자열 (공백도 엄연한 문자!)*/
}
