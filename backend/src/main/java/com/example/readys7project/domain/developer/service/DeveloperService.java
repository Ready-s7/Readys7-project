package com.example.readys7project.domain.developer.service;

import com.example.readys7project.domain.developer.dto.DeveloperDto;
import com.example.readys7project.domain.developer.dto.request.DeveloperProfileRequest;
import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.developer.repository.DeveloperRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.DeveloperException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeveloperService {

    private final DeveloperRepository developerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DeveloperDto> getAllDevelopers() {
        return developerRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeveloperDto getDeveloperById(Long id) {
        Developer developer = developerRepository.findById(id)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));
        return convertToDto(developer);
    }

    @Transactional(readOnly = true)
    public List<DeveloperDto> searchDevelopers(String skill, String location, Double minRating) {
        return developerRepository.searchDevelopers(skill, location, minRating).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeveloperDto updateProfile(DeveloperProfileRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new DeveloperException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != UserRole.DEVELOPER) {
            throw new DeveloperException(ErrorCode.USER_FORBIDDEN);
        }

        Developer developer = developerRepository.findByUser(user)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));

        developer.setTitle(request.getTitle());
        developer.setSkills(request.getSkills());
        developer.setHourlyRate(request.getHourlyRate());
        developer.setResponseTime(request.getResponseTime());
        developer.setPortfolio(request.getPortfolio());

        developer = developerRepository.save(developer);
        return convertToDto(developer);
    }

    @Transactional
    public void updateRating(Long developerId, Double newRating, Integer newReviewCount) {
        Developer developer = developerRepository.findById(developerId)
                .orElseThrow(() -> new DeveloperException(ErrorCode.DEVELOPER_NOT_FOUND));

        developer.setRating(newRating);
        developer.setReviewCount(newReviewCount);
        developerRepository.save(developer);
    }

    private DeveloperDto convertToDto(Developer developer) {
        User user = developer.getUser();
        return DeveloperDto.builder()
                .id(developer.getId())
                .name(user.getName())
                .title(developer.getTitle())
                .rating(developer.getRating())
                .reviewCount(developer.getReviewCount())
                .completedProjects(developer.getCompletedProjects())
                .skills(developer.getSkills())
                .hourlyRate(developer.getHourlyRate())
                .responseTime(developer.getResponseTime())
                .description(user.getDescription())
                .portfolio(developer.getPortfolio())
                .location(user.getLocation())
                .avatarUrl(user.getAvatarUrl())
                .availableForWork(developer.getAvailableForWork())
                .build();
    }
}
