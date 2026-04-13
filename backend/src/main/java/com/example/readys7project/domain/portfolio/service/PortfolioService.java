package com.example.readys7project.domain.portfolio.service;


import com.example.readys7project.domain.portfolio.dto.PortfolioDto;
import com.example.readys7project.domain.portfolio.dto.request.PortfolioRequestDto;
import com.example.readys7project.domain.portfolio.entity.Portfolio;
import com.example.readys7project.domain.portfolio.repository.PortfolioRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.PortfolioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final DeveloperRepository developerRepository;

    // 개발자 포트폴리오 생성 로직
    /**
    // 비즈니스 흐름 로직
    // 1. 로그인 사용자 조회
    // 2. 개발자 권한 검증. 포트폴리오는 개발자만 생성가능
    // 3. 개발자 엔티티 조회. 로그인 사용자에 연결된 개발자 조회
    // 4. 포트폴리오 엔티티 생성
    // 5. 포트폴리오 저장
    // 6. 응답 DTO 변환
    // 7. 반환
     */
    @Transactional
    public PortfolioDto createPortfolio(PortfolioRequestDto request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PortfolioException(ErrorCode.USER_NOT_FOUND));

        if (user.getUserRole() != UserRole.DEVELOPER) {
            throw new PortfolioException(ErrorCode.USER_FORBIDDEN);
        }

        Developer developer = developerRepository.findByUser(user)
                .orElseThrow(() -> new PortfolioException(ErrorCode.DEVELOPER_NOT_FOUND));

        Portfolio portfolio= Portfolio.builder()
                .developer(developer)
                .title(request.title())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .projectUrl(request.projectUrl())
                .skills(request.skills())
                .build();

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);

        return convertToDto(savedPortfolio);
    }


    // 개발자 포트폴리오 수정
    /**
     1. 사용자 검증
     2. 개발자 권한 검증
     3. 개발자 엔티티 조회
     4. 포트폴리오 조회
     5. 본인 포트폴리오 검증
     6. 수정
     7. 저장
     8. 반환
     */
    @Transactional
    public PortfolioDto updatePortfolio(Long developerId, PortfolioRequestDto request, String email) {

        // 로그인 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PortfolioException(ErrorCode.USER_NOT_FOUND));

        // 포트폴리오 수정은 개발자만 가능
        if (user.getUserRole() != UserRole.DEVELOPER) {
            throw new PortfolioException(ErrorCode.USER_FORBIDDEN);

        }

        // 로그인 사용자에 연결된 개발자 엔티티 조회
            Developer developer = developerRepository.findByUser(user)
                    .orElseThrow(() -> new PortfolioException(ErrorCode.DEVELOPER_NOT_FOUND));


        // 수정 대상 포트폴리오 조회
            Portfolio portfolio = portfolioRepository.findByDeveloperId(developerId)
                    .orElseThrow(() -> new PortfolioException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 로그인한 개발자의 포트폴리오가 아니라면 수정 불가
        if (!portfolio.getDeveloper().getId().equals(developer.getId())) {
            throw new PortfolioException(ErrorCode.USER_FORBIDDEN);
        }

        // 포트폴리오 내용 수정
        portfolio.update(
                request.title(),
                request.description(),
                request.imageUrl(),
                request.projectUrl(),
                request.skills()
        );

        // 수정된 포트폴리오 저장
        Portfolio updatedPortfolio = portfolioRepository.save(portfolio);

        return convertToDto(updatedPortfolio);
    }


    // 개발자 포트폴리오 삭제
    /**
     1. 로그인 사용자 조회
     2. 개발자 권한 검증
     3. 로그인 사용자에 연결된 개발자 엔티티 조회
     4. 삭제 대상 포트폴리오 조회
     5. 본인 포트폴리오인지 검증
     6. 삭제
     */
    @Transactional
    public void deletePortfolio(Long developerId, String email){

        // 로그인 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new PortfolioException(ErrorCode.USER_NOT_FOUND));

        // 개발자만 가능.
        if (user.getUserRole() != UserRole.DEVELOPER) {
            throw new PortfolioException(ErrorCode.USER_FORBIDDEN);
        }

        // 사용자 개발자 엔티티로 조회
            Developer developer = developerRepository.findByUser(user)
                    .orElseThrow(() -> new PortfolioException(ErrorCode.DEVELOPER_NOT_FOUND));


        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByDeveloperId(developerId)
                .orElseThrow(() -> new PortfolioException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 자기가 만든 포트폴리오만 삭제가 가능.
        if (!portfolio.getDeveloper().getId().equals(developer.getId())) {
            throw new PortfolioException(ErrorCode.USER_FORBIDDEN);
        }

        portfolioRepository.delete(portfolio);

    }


    // 개발자 포트폴리오 조회
    // 일단 비회원도 조회 가능.
    @Transactional(readOnly = true)
    public Page<PortfolioDto>getPortfolio(Long developerId, String skill, int page, int size){

        Developer developer=developerRepository.findById(developerId)
                .orElseThrow(()->new PortfolioException(ErrorCode.DEVELOPER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page - 1, size);

        return portfolioRepository.searchPortfolios(developerId, skill, pageable)
                .map(this::convertToDto);
    }





// 반환 메서드
    private PortfolioDto convertToDto(Portfolio portfolio) {
        return PortfolioDto.builder()
                .id(portfolio.getId())
                .developerId(portfolio.getDeveloper().getId())
                .title(portfolio.getTitle())
                .description(portfolio.getDescription())
                .imageUrl(portfolio.getImageUrl())
                .projectUrl(portfolio.getProjectUrl())
                .skills(portfolio.getSkills())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .build();
    }


}
