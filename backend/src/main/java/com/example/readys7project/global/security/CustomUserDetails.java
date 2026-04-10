package com.example.readys7project.global.security;

import com.example.readys7project.domain.user.auth.entity.User;
import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Builder // record에서도 @Builder 사용 가능합니다 (Lombok 최신버전 권장)
public record CustomUserDetails(
        User user
) implements UserDetails {

    // record는 이미 'user()' 라는 게터를 자동으로 생성하지만,
    // 기존 코드와의 호환성을 위해 getUser()를 명시적으로 추가할 수 있습니다.
    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 보통 권한은 user.getRole()을 SimpleGrantedAuthority로 변환해서 반환합니다.
        // 현재는 빈 리스트로 두셨으니 그대로 유지하거나 아래처럼 작성할 수 있습니다.
        return List.of(new SimpleGrantedAuthority(user.getUserRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // Security에서 식별자로 이메일을 쓴다면 email을, 이름을 쓴다면 name을 반환하세요.
        return user.getName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getPhoneNumber() {
        return user.getPhoneNumber();
    }

    // 아래 메서드들은 기본값이 true이므로 생략하거나 명시적으로 true를 반환하면 됩니다.
    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}