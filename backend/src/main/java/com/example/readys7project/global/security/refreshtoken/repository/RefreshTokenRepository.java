package com.example.readys7project.global.security.refreshtoken.repository;

import com.example.readys7project.global.security.refreshtoken.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByEmail(String email);

    Optional<RefreshToken> findByToken(String token);

    void deleteByEmail(String email);

    boolean existsByEmail(String email);
}