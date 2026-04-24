package com.example.readys7project.domain.user.admin.repository;

import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.domain.user.admin.enums.AdminStatus;
import com.example.readys7project.domain.user.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin,Long> {

    Optional<Admin> findByUserEmail(String email);

    Optional<Admin> findByUser(User user);

    @Query(value = "select a from Admin a join fetch a.user where a.status = :status",
           countQuery = "select count(a) from Admin a where a.status = :status")
    Page<Admin> findAllByStatus(@Param("status") AdminStatus status, Pageable pageable);
}

