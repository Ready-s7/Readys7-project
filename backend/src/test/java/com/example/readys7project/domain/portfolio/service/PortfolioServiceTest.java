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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @InjectMocks
    private PortfolioService portfolioService;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeveloperRepository developerRepository;

    private User user;
    private Developer developer;
    private Portfolio portfolio;
    private String email = "developer@example.com";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email(email)
                .name("Developer Name")
                .userRole(UserRole.DEVELOPER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        developer = Developer.builder()
                .user(user)
                .title("Full Stack Developer")
                .skills(List.of("Java", "Spring"))
                .build();
        ReflectionTestUtils.setField(developer, "id", 1L);

        portfolio = Portfolio.builder()
                .developer(developer)
                .title("Portfolio Title")
                .description("Portfolio Description")
                .skills(List.of("Java"))
                .build();
        ReflectionTestUtils.setField(portfolio, "id", 1L);
    }

    @Test
    @DisplayName("성공: 포트폴리오 생성")
    void createPortfolio_Success() {
        // given
        PortfolioRequestDto request = PortfolioRequestDto.builder()
                .title("New Title")
                .description("New Description")
                .skills(List.of("Java"))
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));
        given(portfolioRepository.save(any(Portfolio.class))).willReturn(portfolio);

        // when
        PortfolioDto result = portfolioService.createPortfolio(request, email);

        // then
        assertThat(result.title()).isEqualTo(portfolio.getTitle());
        verify(portfolioRepository, times(1)).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("실패: 포트폴리오 생성 - 유저 없음")
    void createPortfolio_UserNotFound() {
        // given
        PortfolioRequestDto request = PortfolioRequestDto.builder().title("Title").build();
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> portfolioService.createPortfolio(request, email))
                .isInstanceOf(PortfolioException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("실패: 포트폴리오 생성 - 권한 없음 (개발자 아님)")
    void createPortfolio_NotDeveloper() {
        // given
        User clientUser = User.builder()
                .email(email)
                .userRole(UserRole.CLIENT)
                .build();
        PortfolioRequestDto request = PortfolioRequestDto.builder().title("Title").build();
        given(userRepository.findByEmail(email)).willReturn(Optional.of(clientUser));

        // when & then
        assertThatThrownBy(() -> portfolioService.createPortfolio(request, email))
                .isInstanceOf(PortfolioException.class)
                .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("성공: 포트폴리오 수정")
    void updatePortfolio_Success() {
        // given
        PortfolioUpdateRequestDto request = PortfolioUpdateRequestDto.builder()
                .title("Updated Title")
                .build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));
        given(portfolioRepository.findById(1L)).willReturn(Optional.of(portfolio));

        // when
        PortfolioDto result = portfolioService.updatePortfolio(1L, request, email);

        // then
        assertThat(result.title()).isEqualTo("Updated Title");
    }

    @Test
    @DisplayName("실패: 포트폴리오 수정 - 본인 아님")
    void updatePortfolio_NotOwner() {
        // given
        Developer otherDeveloper = Developer.builder().build();
        ReflectionTestUtils.setField(otherDeveloper, "id", 2L);

        PortfolioUpdateRequestDto request = PortfolioUpdateRequestDto.builder().title("Title").build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(developerRepository.findByUser(user)).willReturn(Optional.of(otherDeveloper));
        given(portfolioRepository.findById(1L)).willReturn(Optional.of(portfolio));

        // when & then
        assertThatThrownBy(() -> portfolioService.updatePortfolio(1L, request, email))
                .isInstanceOf(PortfolioException.class)
                .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("성공: 포트폴리오 삭제")
    void deletePortfolio_Success() {
        // given
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(portfolioRepository.findById(1L)).willReturn(Optional.of(portfolio));
        given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));

        // when
        portfolioService.deletePortfolio(1L, email);

        // then
        verify(portfolioRepository, times(1)).delete(portfolio);
    }

    @Test
    @DisplayName("성공: 포트폴리오 삭제 - 관리자 권한")
    void deletePortfolio_Admin_Success() {
        // given
        User adminUser = User.builder()
                .email("admin@example.com")
                .userRole(UserRole.ADMIN)
                .build();
        given(userRepository.findByEmail("admin@example.com")).willReturn(Optional.of(adminUser));
        given(portfolioRepository.findById(1L)).willReturn(Optional.of(portfolio));

        // when
        portfolioService.deletePortfolio(1L, "admin@example.com");

        // then
        verify(portfolioRepository, times(1)).delete(portfolio);
    }

    @Test
    @DisplayName("성공: 특정 개발자 포트폴리오 조회")
    void getPortfolio_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Portfolio> page = new PageImpl<>(List.of(portfolio), pageRequest, 1);
        
        given(developerRepository.findById(1L)).willReturn(Optional.of(developer));
        given(portfolioRepository.searchPortfolios(eq(1L), anyString(), any(Pageable.class))).willReturn(page);

        // when
        Page<PortfolioDto> result = portfolioService.getPortfolio(1L, "Java", 1, 10);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo(portfolio.getTitle());
    }

    @Test
    @DisplayName("성공: 포트폴리오 전체 검색")
    void searchPortfolios_Success() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Portfolio> page = new PageImpl<>(List.of(portfolio), pageRequest, 1);

        given(portfolioRepository.searchAllPortfolios(anyString(), any(Pageable.class))).willReturn(page);

        // when
        Page<PortfolioDto> result = portfolioService.searchPortfolios("Java", 1, 10);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo(portfolio.getTitle());
    }

    @Test
    @DisplayName("실패: 포트폴리오 삭제 - 본인 소유가 아니면 예외 발생")
    void deletePortfolio_NotOwner() {
        // given
        Developer otherDeveloper = Developer.builder().build();
        ReflectionTestUtils.setField(otherDeveloper, "id", 2L);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(portfolioRepository.findById(1L)).willReturn(Optional.of(portfolio));
        given(developerRepository.findByUser(user)).willReturn(Optional.of(otherDeveloper));

        // when & then
        assertThatThrownBy(() -> portfolioService.deletePortfolio(1L, email))
                .isInstanceOf(PortfolioException.class)
                .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("성공: 포트폴리오 전체 검색 - 검색 결과가 없으면 빈 페이지 반환")
    void searchPortfolios_EmptyResult() {
        // given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<Portfolio> emptyPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(portfolioRepository.searchAllPortfolios(anyString(), any(Pageable.class))).willReturn(emptyPage);

        // when
        Page<PortfolioDto> result = portfolioService.searchPortfolios("Python", 1, 10);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
