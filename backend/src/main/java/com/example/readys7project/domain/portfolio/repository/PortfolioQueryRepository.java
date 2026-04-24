package com.example.readys7project.domain.portfolio.repository;

import com.example.readys7project.domain.portfolio.entity.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PortfolioQueryRepository {

    // 특정 개발자에 포트폴리오 조회.
    Page<Portfolio> searchPortfolios(Long developerId, String skill, Pageable pageable);

    // 특정 개발자에 포트폴리오 조회가 아닌. 모든 개발자 대상으로 조회
    Page<Portfolio> searchAllPortfolios(String skill, Pageable pageable);
}
