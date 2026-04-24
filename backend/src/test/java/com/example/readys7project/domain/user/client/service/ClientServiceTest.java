package com.example.readys7project.domain.user.client.service;

import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.review.repository.ReviewRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.dto.request.UpdateClientProfileRequestDto;
import com.example.readys7project.domain.user.client.dto.response.ClientProjectsListResponseDto;
import com.example.readys7project.domain.user.client.dto.response.ClientsResponseDto;
import com.example.readys7project.global.dto.PageResponseDto;
import com.example.readys7project.domain.user.client.dto.response.UpdateClientProfileResponseDto;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.enums.ParticipateType;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ClientException;
import com.example.readys7project.global.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @InjectMocks
    private ClientService clientService;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ReviewRepository reviewRepository;

    private User createClientUser(Long id) {
        User user = User.builder()
                .email("test@test.com")
                .name("테스트")
                .password("password")
                .phoneNumber("01012345678")
                .userRole(UserRole.CLIENT)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Client createClient(Long id, User user) {
        Client client = Client.builder()
                .user(user)
                .title("테스트 클라이언트")
                .completedProject(0)
                .rating(0.0)
                .reviewCount(0)
                .participateType(ParticipateType.INDIVIDUAL)
                .build();
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }

    @Nested
    @DisplayName("클라이언트 목록 조회 테스트")
    class GetClientsTest {
        @Test
        @DisplayName("성공: 클라이언트 목록을 조회한다")
        void getClients_success() {
            // given
            User user = createClientUser(1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Pageable pageable = PageRequest.of(1, 10);
            Pageable converted = PageRequest.of(0, 10);

            Client client = createClient(1L, user);
            Page<Client> clientPage = new PageImpl<>(List.of(client), converted, 1);

            given(userRepository.existsById(user.getId())).willReturn(true);
            given(clientRepository.findAllWithPageable(any(Pageable.class))).willReturn(clientPage);

            // when
            PageResponseDto<ClientsResponseDto> result = clientService.getClients(userDetails, pageable);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).name()).isEqualTo(user.getName());
            verify(clientRepository).findAllWithPageable(any(Pageable.class));
        }

        @Test
        @DisplayName("실패: 허용되지 않은 권한의 유저가 목록을 조회하면 예외가 발생한다.")
        void getClients_fail_invalidRole() {
            // given
            User user = User.builder()
                    .userRole(null)
                    .build();

            ReflectionTestUtils.setField(user, "id", 1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Pageable pageable = PageRequest.of(1, 10);

            // when & then
            assertThatThrownBy( () -> clientService.getClients(userDetails, pageable))
                    .isInstanceOf(ClientException.class)
                    .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
        }

        @Test
        @DisplayName("실패: 유저가 존재하지 않으면 예외가 발생한다")
        void getClients_fail_userNotFound() {
            // given
            User user = createClientUser(1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Pageable pageable = PageRequest.of(1, 10);

            given(userRepository.existsById(user.getId())).willReturn(false);

            // when & then
            assertThatThrownBy(() -> clientService.getClients(userDetails, pageable))
                    .isInstanceOf(ClientException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("성공: 페이지 번호가 1일 때 0으로 변환하여 조회한다.")
        void getClients_convertPageable_success() {
            // given
            User user = createClientUser(1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Pageable pageable = PageRequest.of(1, 10);

            given(userRepository.existsById(user.getId())).willReturn(true);
            given(clientRepository.findAllWithPageable(any(Pageable.class)))
                    .willReturn(new PageImpl<>(Collections.emptyList()));

            // when
            clientService.getClients(userDetails, pageable);

            // then
            org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(clientRepository).findAllWithPageable(pageableCaptor.capture());

            assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("클라이언트 상세 조회 테스트")
    class GetClientDetailTest {
        @Test
        @DisplayName("성공: 클라이언트 상세 정보를 조회한다")
        void getClientDetail_success() {
            // given
            User user = createClientUser(1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Client client = createClient(1L, user);

            given(clientRepository.findById(1L)).willReturn(Optional.of(client));

            // when
            ClientsResponseDto result = clientService.getClientDetail(1L, userDetails);

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo(user.getName());
        }

        @Test
        @DisplayName("실패: 클라이언트가 존재하지 않으면 예외가 발생한다")
        void getClientDetail_fail_clientNotFound() {
            // given
            User user = createClientUser(1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);

            given(clientRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> clientService.getClientDetail(1L, userDetails))
                    .isInstanceOf(ClientException.class)
                    .hasMessage(ErrorCode.CLIENT_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("클라이언트 프로필 수정 테스트")
    class UpdateClientProfileTest {
        @Test
        @DisplayName("성공: 본인 프로필을 수정한다")
        void updateClientProfile_success() {
            // given
            User user = createClientUser(1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Client client = createClient(1L, user);
            UpdateClientProfileRequestDto requestDto = new UpdateClientProfileRequestDto("새 제목", ParticipateType.COMPANY);

            given(clientRepository.findById(1L)).willReturn(Optional.of(client));

            // when
            UpdateClientProfileResponseDto result = clientService.updateClientProfile(1L, userDetails, requestDto);

            // then
            assertThat(result.title()).isEqualTo("새 제목");
            assertThat(result.participateType()).isEqualTo(ParticipateType.COMPANY);
        }

        @Test
        @DisplayName("실패: 본인이 아니면 예외가 발생한다")
        void updateClientProfile_fail_forbidden() {
            // given
            User user = createClientUser(1L);
            User otherUser = createClientUser(2L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Client otherClient = createClient(2L, otherUser);
            UpdateClientProfileRequestDto requestDto = new UpdateClientProfileRequestDto("새 제목", ParticipateType.COMPANY);

            given(clientRepository.findById(2L)).willReturn(Optional.of(otherClient));

            // when & then
            assertThatThrownBy(() -> clientService.updateClientProfile(2L, userDetails, requestDto))
                    .isInstanceOf(ClientException.class)
                    .hasMessage(ErrorCode.USER_FORBIDDEN.getMessage());
        }
    }

    @Nested
    @DisplayName("내 프로젝트 목록 조회 테스트")
    class GetMyProjectsTest {
        @Test
        @DisplayName("성공: 내 프로젝트 목록을 조회한다")
        void getMyProjects_success() {
            // given
            User user = createClientUser(1L);
            CustomUserDetails userDetails = new CustomUserDetails(user);
            Client client = createClient(1L, user);
            Pageable pageable = PageRequest.of(1, 10);

            given(clientRepository.findByUser(user)).willReturn(Optional.of(client));
            given(projectRepository.findByClientWithPageable(any(), any())).willReturn(new PageImpl<>(Collections.emptyList()));

            // when
            PageResponseDto<ClientProjectsListResponseDto> result = clientService.getMyProjects(userDetails, pageable);

            // then
            assertThat(result.content()).isEmpty();
        }
    }

    @Nested
    @DisplayName("평점 업데이트 테스트")
    class UpdateRatingTest {
        @Test
        @DisplayName("성공: 평점을 업데이트한다")
        void updateRating_success() {
            // given
            User user = createClientUser(1L);
            Client client = createClient(1L, user);

            given(clientRepository.findById(1L)).willReturn(Optional.of(client));
            given(reviewRepository.getClientRatingSummary(1L)).willReturn(List.<Object[]>of(new Object[]{4.5, 1L}));

            //when
            clientService.updateRating(1L);

            // then
            assertThat(client.getRating()).isEqualTo(4.5);
            assertThat(client.getReviewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공: 새로운 평점과 리뷰 개수가 객체에 정확히 반영된다")
        void updateRating_dataIntegrity_success() {
            // given
            User user = createClientUser(1L);
            Client client = createClient(1L, user);

            given(clientRepository.findById(1L)).willReturn(Optional.of(client));
            given(reviewRepository.getClientRatingSummary(1L)).willReturn(List.<Object[]>of(new Object[]{4.8, 10L}));

            // when
            clientService.updateRating(1L);

            // then
            assertThat(client.getRating()).isEqualTo(4.8);
            assertThat(client.getReviewCount()).isEqualTo(10);
        }
    }
}
