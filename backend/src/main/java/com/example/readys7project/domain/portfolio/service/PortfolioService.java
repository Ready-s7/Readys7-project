package com.example.readys7project.domain.portfolio.service;


import com.example.readys7project.domain.portfolio.dto.PortfolioDto;
import com.example.readys7project.domain.portfolio.dto.request.PortfolioRequestDto;
import com.example.readys7project.domain.portfolio.dto.request.PortfolioUpdateRequestDto;
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

        User user = findUserByEmail(email);
        validatePortfolioWriterRole(user);

        Developer developer = findDeveloperByUser(user);

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
    public PortfolioDto updatePortfolio(Long portfolioId, PortfolioUpdateRequestDto request, String email) {
        validateUpdateRequest(request);

        // 로그인 검증.
        User user = findUserByEmail(email);

        // 역할 검증.
        validatePortfolioWriterRole(user);

        Developer developer = findDeveloperByUser(user);
        Portfolio portfolio = findPortfolio(portfolioId);

        // 너가 이 포트폴리오 갖고 있는 사람인지 검증.
        validatePortfolioOwner(developer, portfolio);


        portfolio.portfolioUpdate(request);

        return convertToDto(portfolio);
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
        User user = findUserByEmail(email);

        Portfolio portfolio = findPortfolio(developerId);
        // 관리자는 삭제 가능.
        if(user.getUserRole()!=UserRole.ADMIN){
            validatePortfolioWriterRole(user);

            Developer developer = findDeveloperByUser(user);
            validatePortfolioOwner(developer, portfolio);
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

    // 공통 메서드들
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new PortfolioException(ErrorCode.USER_NOT_FOUND));
    }

    private Developer findDeveloperByUser(User user) {
        return developerRepository.findByUser(user)
                .orElseThrow(() -> new PortfolioException(ErrorCode.DEVELOPER_NOT_FOUND));
    }

    private Portfolio findPortfolio(Long portfolioId) {
        return portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioException(ErrorCode.PORTFOLIO_NOT_FOUND));
    }

    // 역할 검증.
    private void validatePortfolioWriterRole(User user) {
        if (user.getUserRole() != UserRole.DEVELOPER) {
            throw new PortfolioException(ErrorCode.USER_FORBIDDEN);
        }
    }

    // 너가 이 포트폴리오 주인인지 검증
    private void validatePortfolioOwner(Developer developer, Portfolio portfolio) {
        if (!portfolio.getDeveloper().getId().equals(developer.getId())) {
            throw new PortfolioException(ErrorCode.USER_FORBIDDEN);
        }
    }

    // 모든 수정 필드가 null이면 업데이트할 내용이 없는 요청으로 본다.
    // 선택적으로 들어온 문자열 필드는 공백만 포함한 값인지 검증한다.
    private void validateUpdateRequest(PortfolioUpdateRequestDto request) {
        if ((request.title() == null || request.title().isBlank()) &&
                (request.description() == null || request.description().isBlank()) &&
                (request.imageUrl() == null || request.imageUrl().isBlank()) &&
                (request.projectUrl() == null || request.projectUrl().isBlank()) &&
                request.skills() == null )
        {
            throw new PortfolioException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }



    }


}
