package com.example.readys7project.domain.user.admin.repository;

import com.example.readys7project.domain.user.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin,Long> {
}
