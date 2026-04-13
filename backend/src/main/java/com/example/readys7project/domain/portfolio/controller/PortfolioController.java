package com.example.readys7project.domain.portfolio.controller;


import com.example.readys7project.domain.portfolio.dto.PortfolioDto;
import com.example.readys7project.domain.portfolio.dto.request.PortfolioRequestDto;
import com.example.readys7project.domain.portfolio.service.PortfolioService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    // 개발자 포트폴리오 생성
    @PostMapping("/v1/portfolios")
    public ResponseEntity<ApiResponseDto<PortfolioDto>> createPortfolio(
            @Valid @RequestBody PortfolioRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.CREATED, portfolioService.createPortfolio(request, email))
        );
    }

    // 개발자 포트폴리오 수정
    @PatchMapping(value = "/v1/portfolios", params = "developerId")
    public ResponseEntity<ApiResponseDto<PortfolioDto>> updatePortfolio(
            @RequestParam Long developerId,
            @Valid @RequestBody PortfolioRequestDto request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, portfolioService.updatePortfolio(developerId, request, email))
        );
    }

    // 개발자 포트폴리오 삭제
    @DeleteMapping(value = "/v1/portfolios", params = "developerId")
    public ResponseEntity<ApiResponseDto<Void>> deletePortfolio(
            @RequestParam Long developerId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String email = customUserDetails.getEmail();
        portfolioService.deletePortfolio(developerId, email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }


    // 개발자 포트폴리오 조회
    @GetMapping(value = "/v1/portfolios", params = "developerId")
    public ResponseEntity<ApiResponseDto<PortfolioDto>> getPortfolio(
            @RequestParam Long developerId
    ) {
        return ResponseEntity.ok(
                ApiResponseDto.success(HttpStatus.OK, portfolioService.getPortfolio(developerId))
        );
    }

}
