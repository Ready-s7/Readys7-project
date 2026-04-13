package com.example.readys7project.domain.portfolio.repository;


import com.example.readys7project.domain.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio,Long> {

    // 단순 조회
    Optional<Portfolio> findByDeveloperId(Long developerId);
}
