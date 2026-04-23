package com.example.readys7project.domain.user.developer.service;

import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.review.repository.ReviewQueryRepository;
import com.example.readys7project.domain.review.repository.ReviewRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.developer.dto.DeveloperResponseDto;
import com.example.readys7project.domain.user.developer.dto.request.DeveloperProfileRequestDto;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.DeveloperException;
import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.user.client.entity.Client;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeveloperServiceTest {

    @InjectMocks
    private DeveloperService developerService;

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Test
    @DisplayName("전체 개발자 목록 조회 성공")
    void getAllDevelopers_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        User user = createUser(1L, "dev@test.com", UserRole.DEVELOPER);
        Developer developer = createDeveloper(1L, user);
        Page<Developer> developerPage = new PageImpl<>(List.of(developer));

        given(developerRepository.findAllWithUser(pageable)).willReturn(developerPage);

        // when
        Page<DeveloperResponseDto> result = developerService.getAllDevelopers(pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("전체 개발자 목록 조회 - 빈 목록")
    void getAllDevelopers_EmptyResult() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Developer> emptyPage = new PageImpl<>(Collections.emptyList());

        given(developerRepository.findAllWithUser(pageable)).willReturn(emptyPage);

        // when
        Page<DeveloperResponseDto> result = developerService.getAllDevelopers(pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("개발자 상세 조회 성공")
    void getDeveloperById_Success() {
        // given
        User user = createUser(1L, "dev@test.com", UserRole.DEVELOPER);
        Developer developer = createDeveloper(1L, user);

        given(developerRepository.findById(1L)).willReturn(Optional.of(developer));

        // when
        DeveloperResponseDto result = developerService.getDeveloperById(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("개발자 상세 조회 실패 - 존재하지 않는 개발자")
    void getDeveloperById_NotFound() {
        // given
        given(developerRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> developerService.getDeveloperById(1L))
                .isInstanceOf(DeveloperException.class)
                .hasMessage(ErrorCode.DEVELOPER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("개발자 검색 성공")
    void searchDevelopers_Success() {
        // given
        List<String> skills = List.of("Java", "Spring");
        Double minRating = 4.0;
        Pageable pageable = PageRequest.of(0, 10);
        User user = createUser(1L, "dev@test.com", UserRole.DEVELOPER);
        Developer developer = createDeveloper(1L, user);
        Page<Developer> developerPage = new PageImpl<>(List.of(developer));

        given(developerRepository.searchDevelopers(skills, minRating, pageable)).willReturn(developerPage);

        // when
        Page<DeveloperResponseDto> result = developerService.searchDevelopers(skills, minRating, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("개발자 프로필 수정 성공")
    void updateProfile_Success() {
        // given
        String email = "dev@test.com";
        DeveloperProfileRequestDto request = new DeveloperProfileRequestDto(
                "Updated Title", List.of("Java", "Spring"), 50000, 100000, "1시간", true
        );
        User user = createUser(1L, email, UserRole.DEVELOPER);
        Developer developer = createDeveloper(1L, user);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));

        // when
        DeveloperResponseDto result = developerService.updateProfile(request, email);

        // then
        assertThat(result.title()).isEqualTo("Updated Title");
        assertThat(result.skills()).containsExactly("Java", "Spring");
    }

    @Test
    @DisplayName("개발자 프로필 수정 실패 - 데이터 모두 Null")
    void updateProfile_AllDataNull() {
        // given
        DeveloperProfileRequestDto request = new DeveloperProfileRequestDto(
                null, null, null, null, null, null
        );

        // when & then
        assertThatThrownBy(() -> developerService.updateProfile(request, "dev@test.com"))
                .isInstanceOf(DeveloperException.class)
                .hasMessage(ErrorCode.SKILL_UPDATE_DATA_NULL.getMessage());
    }

    @Test
    @DisplayName("평점 업데이트 성공")
    void updateRating_Success() {
        // given
        User user = createUser(1L, "dev@test.com", UserRole.DEVELOPER);
        Developer developer = createDeveloper(1L, user);

        given(developerRepository.findById(1L)).willReturn(Optional.of(developer));
        given(reviewRepository.getDeveloperRatingSummary(1L)).willReturn(new Object[]{4.5, 10L});

        // when
        developerService.updateRating(1L);

        // then
        assertThat(developer.getRating()).isEqualTo(4.5);
        assertThat(developer.getReviewCount()).isEqualTo(10);
        verify(developerRepository).findById(1L);
    }

    @Test
    @DisplayName("내 프로젝트 목록 조회 성공")
    void getMyProjects_Success() {
        // given
        String email = "dev@test.com";
        Pageable pageable = PageRequest.of(0, 10);
        User user = createUser(1L, email, UserRole.DEVELOPER);
        Developer developer = createDeveloper(1L, user);
        
        Project project = createProject(1L);
        Page<Project> projectPage = new PageImpl<>(List.of(project));

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(developerRepository.findByUser(user)).willReturn(Optional.of(developer));
        given(developerRepository.findMyProjects(developer, pageable)).willReturn(projectPage);

        // when
        Page<ProjectResponseDto> result = developerService.getMyProjects(email, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Test Project");
    }

    @Test
    @DisplayName("내 프로젝트 목록 조회 실패 - 존재하지 않는 유저")
    void getMyProjects_UserNotFound() {
        // given
        String email = "notexist@test.com";
        Pageable pageable = PageRequest.of(0, 10);

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> developerService.getMyProjects(email, pageable))
                .isInstanceOf(DeveloperException.class);
    }

    private User createUser(Long id, String email, UserRole role) {
        User user = User.builder()
                .email(email)
                .name("Test User")
                .userRole(role)
                .password("password")
                .phoneNumber("01012345678")
                .description("Test Description")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Developer createDeveloper(Long id, User user) {
        Developer developer = Developer.builder()
                .user(user)
                .title("Fullstack Developer")
                .rating(0.0)
                .reviewCount(0)
                .completedProjects(0)
                .skills(Collections.singletonList("Java"))
                .minHourlyPay(30000)
                .maxHourlyPay(50000)
                .responseTime("1시간")
                .availableForWork(true)
                .build();
        ReflectionTestUtils.setField(developer, "id", id);
        return developer;
    }

    private Project createProject(Long id) {
        User clientUser = createUser(2L, "client@test.com", UserRole.CLIENT);
        Client client = Client.builder().user(clientUser).build();
        ReflectionTestUtils.setField(client, "id", 1L);

        Category category = Category.builder().name("Web").build();
        ReflectionTestUtils.setField(category, "id", 1L);

        Project project = Project.builder()
                .client(client)
                .category(category)
                .title("Test Project")
                .description("Test Description")
                .minBudget(1000000L)
                .maxBudget(5000000L)
                .duration(30)
                .skills(List.of("Java", "Spring"))
                .maxProposalCount(10)
                .build();
        ReflectionTestUtils.setField(project, "id", id);
        return project;
    }
}
