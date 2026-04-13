package com.example.readys7project.domain.user.developer.service;

import com.example.readys7project.domain.project.dto.ProjectDto;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.user.developer.dto.DeveloperDto;
import com.example.readys7project.domain.user.developer.dto.request.DeveloperProfileRequestDto;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.DeveloperException;
import com.example.readys7project.global.exception.domain.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeveloperService {

    private final DeveloperRepository developerRepository;
    private final UserRepository userRepository;

    // 전체 개발자 목록
    public Page<DeveloperDto> getAllDevelopers(Pageable pageable) {
        return developerRepository.findAllWithUser(pageable)
                .map(this::convertToDto);
    }

    // 개발자 상세 조회
    public DeveloperDto getDeveloperById(Long developerId) {
        Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));
        return convertToDto(developer);
    }

    // 개발자 검색 (skill, minRating)
    public Page<DeveloperDto> searchDevelopers(String skill, Double minRating, Pageable pageable) {
        return developerRepository.searchDevelopers(skill, minRating, pageable)
                .map(this::convertToDto);
    }

    // 개발자 프로필 수정 (DEVELOPER 전용)
    @Transactional
    public DeveloperDto updateProfile(DeveloperProfileRequestDto request, String userEmail) {
        Developer developer = getDeveloperByEmail(userEmail);

        developer.updateProfile(request.title(), request.skills(), request.minHourlyPay(),
                request.maxHourlyPay(), request.responseTime(), request.availableForWork());

        return convertToDto(developer);
    }

    // 평점 업데이트
    @Transactional
    public void updateRating(Long developerId, Double newRating, Integer newReviewCount) {
        Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));

        developer.updateRating(newRating, newReviewCount);
    }

    // 내 프로젝트 목록 조회
    public Page<ProjectDto> getMyProjects(String userEmail, Pageable pageable) {
        Developer developer = getDeveloperByEmail(userEmail);

        return developerRepository.findMyProjects(developer, pageable)
                .map(this::convertToProjectDto);
    }

    // 공통 메서드 추출 (검증)
    private Developer getDeveloperByEmail(String userEmail) {
        // 1. JWT에서 파싱된 이메일로 DB 조회 -> User 존재 여부 확인
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 해당 User의 역할이 DEVELOPER인지 확인
        if (user.getUserRole() != UserRole.DEVELOPER) {
            throw new UserException(ErrorCode.USER_FORBIDDEN);
        }

        // 3. 해당 유저와 연결된 Developer 엔티티 조회 후 반환
        return developerRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));
    }

    // Developer 엔티티 → DeveloperDto 변환 (개발자 프로필 조회 응답용)
    private DeveloperDto convertToDto(Developer developer) {
        User user = developer.getUser();
        return new DeveloperDto(
                developer.getId(),
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
                developer.getParticipateType()
        );
    }

    // Project 엔티티 → ProjectDto 변환 (내 프로젝트 목록 조회 응답용)
    private ProjectDto convertToProjectDto(Project project) {
        return new ProjectDto(
                project.getId(),
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
