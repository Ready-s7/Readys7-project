package com.example.readys7project.domain.project.service;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.dto.ProjectResponseDto;
import com.example.readys7project.domain.project.dto.request.ProjectCreateRequestDto;
import com.example.readys7project.domain.project.dto.request.ProjectUpdateRequestDto;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.enums.ProjectStatus;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.global.exception.domain.ProjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectServiceTest {

    @InjectMocks
    private ProjectService projectService;

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private CategoryRepository categoryRepository;

    private User clientUser;
    private Client client;
    private Category category;
    private Project project;
    private final String email = "client@example.com";

    @BeforeEach
    void setUp() {
        clientUser = User.builder().email(email).name("Client").userRole(UserRole.CLIENT).build();
        ReflectionTestUtils.setField(clientUser, "id", 1L);

        client = Client.builder().user(clientUser).title("Company").rating(4.5).build();
        ReflectionTestUtils.setField(client, "id", 1L);

        category = Category.builder().name("Web").build();
        ReflectionTestUtils.setField(category, "id", 1L);
        ReflectionTestUtils.setField(category, "isDeleted", false);

        project = Project.builder()
                .client(client).category(category).title("Project").description("Desc")
                .minBudget(100L).maxBudget(500L).duration(30).maxProposalCount(10)
                .skills(List.of("Java")).build();
        ReflectionTestUtils.setField(project, "id", 1L);
        ReflectionTestUtils.setField(project, "status", ProjectStatus.OPEN);
        ReflectionTestUtils.setField(project, "currentProposalCount", 0);
    }

    @Nested
    @DisplayName("프로젝트 생성")
    class CreateProject {
        @Test
        @DisplayName("성공: 프로젝트 생성")
        void createProject_Success() {
            ProjectCreateRequestDto request = new ProjectCreateRequestDto("Project", "Desc", 1L, 100L, 500L, 30, List.of("Java"), 10);
            given(userRepository.findByEmail(email)).willReturn(Optional.of(clientUser));
            given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(projectRepository.save(any(Project.class))).willReturn(project);

            ProjectResponseDto result = projectService.createProject(request, email);
            assertThat(result.title()).isEqualTo("Project");
        }

        @Test
        @DisplayName("실패: 삭제된 카테고리")
        void createProject_DeletedCategory_Fail() {
            ProjectCreateRequestDto request = new ProjectCreateRequestDto("P", "D", 1L, 100L, 500L, 30, List.of("J"), 10);
            ReflectionTestUtils.setField(category, "isDeleted", true);
            given(userRepository.findByEmail(email)).willReturn(Optional.of(clientUser));
            given(clientRepository.findByUser(clientUser)).willReturn(Optional.of(client));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

            assertThatThrownBy(() -> projectService.createProject(request, email)).isInstanceOf(ProjectException.class);
        }
    }

    @Nested
    @DisplayName("프로젝트 조회")
    class GetProject {
        @Test
        @DisplayName("성공: 전체 목록")
        void getAllProjects_Success() {
            given(projectRepository.findAll()).willReturn(List.of(project));
            List<ProjectResponseDto> result = projectService.getAllProjects();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("성공: 단건 조회")
        void getProjectById_Success() {
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));
            ProjectResponseDto result = projectService.getProjectById(1L);
            assertThat(result.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("실패: 없는 프로젝트")
        void getProjectById_NotFound() {
            given(projectRepository.findById(1L)).willReturn(Optional.empty());
            assertThatThrownBy(() -> projectService.getProjectById(1L)).isInstanceOf(ProjectException.class);
        }

        @Test
        @DisplayName("성공: 검색")
        void searchProjects_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Project> page = new PageImpl<>(List.of(project), pageable, 1);
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
            given(projectRepository.searchProjects(any(), any(), any(), any(), eq(pageable))).willReturn(page);

            Page<ProjectResponseDto> result = projectService.searchProjects("K", 1L, "OPEN", null, pageable);
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("프로젝트 수정 및 삭제")
    class UpdateDelete {
        @Test
        @DisplayName("성공: 수정")
        void updateProject_Success() {
            ProjectUpdateRequestDto request = new ProjectUpdateRequestDto("New", "D", 1L, 100L, 500L, 30, List.of("J"), 10);
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));
            given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

            ProjectResponseDto result = projectService.updateProject(1L, request);
            assertThat(result.title()).isEqualTo("New");
        }

        @Test
        @DisplayName("성공: 삭제")
        void deleteProject_Success() {
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));
            projectService.deleteProject(1L);
            verify(projectRepository).delete(project);
        }
    }

    @Nested
    @DisplayName("기능 및 상태")
    class StatusAndFunc {
        @Test
        @DisplayName("성공: 제안 수 증가")
        void incrementProposalCount_Success() {
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));
            projectService.incrementProposalCount(1L);
            assertThat(project.getCurrentProposalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패: 상태 변경(IN_PROGRESS)은 수동으로 허용되지 않는다")
        void changeStatus_InProgress_Forbidden() {
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.changeProjectStatus(1L, "IN_PROGRESS"))
                    .isInstanceOf(ProjectException.class);
        }

        @Test
        @DisplayName("성공: ADMIN 강제 상태 변경")
        void changeStatus_Admin_Success() {
            User admin = User.builder().email("admin@test.com").userRole(UserRole.ADMIN).build();
            given(userRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));
            given(projectRepository.findById(1L)).willReturn(Optional.of(project));

            ProjectResponseDto result = projectService.changeProjectStatus(1L, "CANCELLED");
            assertThat(result.status()).isEqualTo("CANCELLED");
        }
    }
}
