package com.example.readys7project.domain.user.developer.service;

import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.review.repository.ReviewRepository;
import com.example.readys7project.domain.user.developer.dto.DeveloperResponseDto;
import com.example.readys7project.domain.user.developer.dto.request.DeveloperProfileRequestDto;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.DeveloperException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional(readOnly = true)
public class DeveloperService {

    private final DeveloperRepository developerRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    
    public DeveloperService(
            DeveloperRepository developerRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository
    ) {
        this.developerRepository = developerRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }


    // 전체 개발자 목록
    public Page<DeveloperResponseDto> getAllDevelopers(Pageable pageable) {
        return developerRepository.findAllWithUser(pageable)
                .map(this::convertToDto);
    }

    // 개발자 상세 조회
    public DeveloperResponseDto getDeveloperById(Long developerId) {
        Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));
        return convertToDto(developer);
    }

    // 개발자 검색 (skill, minRating)
    public Page<DeveloperResponseDto> searchDevelopers(List<String> skills, Double minRating, Pageable pageable) {
        return developerRepository.searchDevelopers(skills, minRating, pageable)
                .map(this::convertToDto);
    }

    // 개발자 프로필 수정 (DEVELOPER 전용)
    @Transactional
    @CacheEvict(value = "globalSearch", allEntries = true)
    public DeveloperResponseDto updateProfile(DeveloperProfileRequestDto request, String userEmail) {
        // 모든 필드가 Null(또는 빈값)인지 검증 -> 공통 메서드 4번
        validateUpdateData(request);

        User user = getUserByEmail(userEmail);
        validateUserRole(user);
        Developer developer = getDeveloperByUser(user);

        developer.updateProfile(request.title(), request.skills(), request.minHourlyPay(),
                request.maxHourlyPay(), request.responseTime(), request.availableForWork());
        return convertToDto(developer);
    }

    // 평점 업데이트 (ReviewService 같은 내부 클래스에서만 호출가능)
    @Transactional
    @CacheEvict(value = "globalSearch", allEntries = true)
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,  // 이 예외 발생 시 재시도
            maxAttempts = 3,                                      // 최대 3회 시도
            backoff = @Backoff(delay = 100, multiplier = 2.0)    // 100ms → 200ms → 400ms 간격
    )
    public void updateRating(Long developerId) {

        Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));

        List<Object[]> results = reviewRepository.getDeveloperRatingSummary(developerId);
        
        if (results.isEmpty() || results.get(0) == null) {
            developer.updateRating(0.0, 0);
            return;
        }

        Object[] summary = results.get(0);
        Double avg = (Double) summary[0];
        Long count = (Long) summary[1];

        double avgVal = (avg != null) ? avg : 0.0;
        int countVal = (count != null) ? count.intValue() : 0;

        double rounded = Math.round(avgVal * 10) / 10.0;
        developer.updateRating(rounded, countVal);
    }

    @Recover
    public void recoverUpdateDeveloperRating(OptimisticLockingFailureException e, Long developerId) {
        throw new DeveloperException(ErrorCode.REVIEW_RATING_UPDATE_FAILED_DEVELOPER);
    }

    // 내 프로젝트 목록 조회
    public Page<ProjectResponseDto> getMyProjects(String userEmail, Pageable pageable) {
        User user = getUserByEmail(userEmail);
        validateUserRole(user);
        Developer developer = getDeveloperByUser(user);

        return developerRepository.findMyProjects(developer, pageable)
                .map(this::convertToProjectDto);
    }


    // 공통 메서드 1. 이메일로 User 조회
    private User getUserByEmail(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new DeveloperException(ErrorCode.USER_NOT_FOUND));
    }

    // 공통 메서드 2. User 역할 검증
    private void validateUserRole(User user) {
        if (user.getUserRole() != UserRole.DEVELOPER) {
            throw new DeveloperException(ErrorCode.USER_FORBIDDEN);
        }
    }

    // 공통 메서드 3. 역할 검증된 User로 Developer 조회
    private Developer getDeveloperByUser(User user) {
        return developerRepository.findByUser(user)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));
    }

    // 공통 메서드 4. 수정 요청 데이터가 모두 비어있는지 확인 (입구컷)
    private void validateUpdateData(DeveloperProfileRequestDto request) {
        boolean isAllNull = (request.title() == null || request.title().isBlank()) &&
                (request.skills() == null || request.skills().isEmpty()) &&
                request.minHourlyPay() == null &&
                request.maxHourlyPay() == null &&
                (request.responseTime() == null || request.responseTime().isBlank()) &&
                request.availableForWork() == null;
        if (isAllNull) {
            throw new DeveloperException(ErrorCode.SKILL_UPDATE_DATA_NULL);
        }
    }

    // Developer 엔티티 → DeveloperDto 변환 (개발자 프로필 조회 응답용)
    private DeveloperResponseDto convertToDto(Developer developer) {
        User user = developer.getUser();
        return new DeveloperResponseDto(
                developer.getId(),
                user.getId(),
                user.getName(),
                developer.getTitle(),
                developer.getRating(),
                developer.getReviewCount(),
                developer.getCompletedProjects(),
                developer.getSkills(),
                developer.getMinHourlyPay(),
                developer.getMaxHourlyPay(),
                developer.getResponseTime(),
                user.getDescription(),
                developer.getAvailableForWork(),
                developer.getParticipateType(),
                developer.getCreatedAt(),
                developer.getUpdatedAt()
        );
    }

    // Project 엔티티 → ProjectResponseDto 변환 (내 프로젝트 목록 조회 응답용)
    private ProjectResponseDto convertToProjectDto(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getClient().getId(),
                project.getClient().getUser().getId(),
                project.getTitle(),
                project.getDescription(),
                project.getCategory().getName(),
                project.getMinBudget(),
                project.getMaxBudget(),
                project.getDuration(),
                project.getSkills(),
                project.getStatus().name(),
                project.getCurrentProposalCount(),
                project.getMaxProposalCount(),
                project.getClient().getUser().getName(),
                project.getClient().getRating(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
