package com.example.readys7project.domain.user.developer.repository;

import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Long>, DeveloperQueryRepository {
    Optional<Developer> findByUser(User user);
    Optional<Developer> findByUserEmail(String email);
}
