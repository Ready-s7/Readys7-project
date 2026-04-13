package com.example.readys7project.domain.portfolio.repository;

import com.example.readys7project.domain.portfolio.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PortfolioQueryRepository {

    Page<Portfolio> searchPortfolios(Long developerId, String skill, Pageable pageable);
}
