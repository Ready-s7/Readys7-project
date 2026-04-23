package com.example.readys7project.domain.user.client.service;

import com.example.readys7project.domain.project.entity.Project;
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
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ClientException;
import com.example.readys7project.global.security.CustomUserDetails;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;

    public ClientService(
            ClientRepository clientRepository,
            UserRepository userRepository,
            ProjectRepository projectRepository,
            ReviewRepository reviewRepository
    ) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)

    // 클라이언트 목록 조회
    public PageResponseDto<ClientsResponseDto> getClients(
            CustomUserDetails customUserDetails,
            Pageable pageable
    ) {

        // 로그인 유저 정보 가져오기
        User user = customUserDetails.getUser();

        // 로그인 유저 권한 체크
        validateRole(user);

        // 유저의 정보가 존재하는지 유저레포에 확인
        boolean exist = userRepository.existsById(user.getId());
        if (!exist) {
            throw new ClientException(ErrorCode.USER_NOT_FOUND);
        }

        // Pageable 세팅
        Pageable converted = convertPageable(pageable);

        // Client 페이징 조회
        Page<Client> clientPage = clientRepository.findAllWithPageable(converted);

        // ClientPage를 ClientsResponseDto로 변환
        List<ClientsResponseDto> clientList = clientPage.getContent().stream()
                .map(this::convertToDto)
                .toList();

        // PageResponseDto 공통 페이징 DTO로 반환
        return PageResponseDto.of(clientPage, clientList);
    }

    // 클라이언트 상세 조회
    @Transactional(readOnly = true)
    public ClientsResponseDto getClientDetail(Long clientId, CustomUserDetails customUserDetails) {

        // 로그인 유저 정보 가져오기
        User user = customUserDetails.getUser();

        // 로그인 유저 권한 체크
        validateRole(user);

        // 클라이언트 레포에 존재여부 확인
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientException(ErrorCode.CLIENT_NOT_FOUND));

        // Dto 반환
        return convertToDto(client);
    }

    @Transactional
    public UpdateClientProfileResponseDto updateClientProfile(
            Long clientId,
            CustomUserDetails customUserDetails,
            UpdateClientProfileRequestDto updateClientProfileRequestDto
    ) {

        // 로그인한 유저 정보 가져오기
        User user = customUserDetails.getUser();

        // 로그인 유저가 Client가 아닐 경우 예외 던지기
        if(!user.getUserRole().equals(UserRole.CLIENT)) {
            throw new ClientException(ErrorCode.USER_FORBIDDEN);
        }

        // 클라이언트 레포에 존재하는지 확인
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientException(ErrorCode.CLIENT_NOT_FOUND));

        // 본인이 맞는지 확인
        if(!client.getUser().getId().equals(user.getId())) {
            throw new ClientException(ErrorCode.USER_FORBIDDEN);
        }

        // 업데이트 메서드에 넘기기
        client.updateProfile(
                updateClientProfileRequestDto.title(),
                updateClientProfileRequestDto.participateType());

        clientRepository.save(client);

        return UpdateClientProfileResponseDto.builder()
                .clientId(client.getId())
                .title(client.getTitle())
                .participateType(client.getParticipateType())
                .updatedAt(client.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDto<ClientProjectsListResponseDto> getMyProjects(
            CustomUserDetails customUserDetails, Pageable pageable
    ) {

        // 로그인 유저 정보 가져오기
        User user = customUserDetails.getUser();

        validateRole(user);

        // 페이징 세팅
        Pageable converted = convertPageable(pageable);

        // 클라이언트 유저 찾아보기
        Client client = clientRepository.findByUser(user)
                .orElseThrow(() -> new ClientException(ErrorCode.CLIENT_NOT_FOUND));

        // ClientId로 프로젝트 페이징 조회
        Page<Project> projectPage = projectRepository.findByClientWithPageable(client.getId(), converted);

        // Project를 ClientProjectsListResponseDto 변환
        List<ClientProjectsListResponseDto> projectList = projectPage.getContent().stream()
                .map(project -> ClientProjectsListResponseDto.builder()
                        .id(project.getId())
                        .title(project.getTitle())
                        .description(project.getDescription())
                        .category(project.getCategory().getName())
                        .minBudget(project.getMinBudget())
                        .maxBudget(project.getMaxBudget())
                        .duration(project.getDuration())
                        .status(project.getStatus())
                        .currentProposalCount(project.getCurrentProposalCount())
                        .maxProposalCount(project.getMaxProposalCount())
                        .skills(project.getSkills())
                        .createdAt(project.getCreatedAt())
                        .build())
                .toList();

        // Dto 반환
        return PageResponseDto.of(projectPage, projectList);
    }


    // 평점 업데이트 (ReviewService 같은 내부 클래스에서만 호출가능)
    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public void updateRating(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientException(ErrorCode.CLIENT_NOT_FOUND));

        Object[] summary = (Object[]) reviewRepository.getClientRatingSummary(clientId);
        
        // summary 자체가 [avg, count] 배열입니다.
        Double avg = (Double) summary[0];
        Long count = (Long) summary[1];

        // ✅ 리뷰가 0개면 avg는 null이 오며, 이때 0.0으로 처리합니다.
        double avgVal = (avg != null) ? avg : 0.0;
        int countVal = (count != null) ? count.intValue() : 0;

        double rounded = Math.round(avgVal * 10) / 10.0;
        client.updateRating(rounded, countVal);
    }

    @Recover
    public void recoverUpdateClientRating(OptimisticLockingFailureException e, Long clientId) {
        throw new ClientException(ErrorCode.REVIEW_RATING_UPDATE_FAILED_CLIENT);
    }

    // 공통 페이징
    public static Pageable convertPageable(Pageable pageable) {
        return PageRequest.of(
                Math.max(0, pageable.getPageNumber() -1),
                pageable.getPageSize(),
                pageable.getSort()
        );
        /* Math.max(a, b) -> 자바에서 제공하는 Math 함수
           "둘 중에 더 큰놈만 살아남는다"는 원리
           a와 b라는 두 값을 넣으면, 둘을 비교해서 더 큰 값을 결과로 돌려줌!

           PageRequest.of() -> 내가 원하는 페이지 데이터를 가져오기 위한 "주문서"
           PageRequest안에 있는 정적 팩토리 메서드인데, 객체를 직접 생성하지 않고, .of()로 간편하게 객체 생성

           주요 파라미터

           1. PageRequest.of(int page, int size)
           -> page : 몇 번째 페이지를 가져올 것인지, size -> 한 페이지에 몇개의 데이터를 담을 것인지
           2. PageRequest.of(int page, int size, Sort sort)
           -> 1번에서 정렬 조건 (sort)를 추가한 것
           3. PageRequest.of(int page, int size, Direction direction, String.... properties)
           -> 정렬 방향(오름차순/내림차순)과 정렬한 기준 필드명을 직접 지정
           */
    }

    // 로그인 유저 권한 체크
    private void validateRole(User user) {
        UserRole userRole = user.getUserRole();
        if(!(UserRole.ADMIN.equals(userRole) || UserRole.CLIENT.equals(userRole) || UserRole.DEVELOPER.equals(userRole))) {
            throw new ClientException(ErrorCode.USER_FORBIDDEN);
        }
    }

    private ClientsResponseDto convertToDto(Client client) {
        return ClientsResponseDto.builder()
                .id(client.getId())
                .userId(client.getUser().getId()) // 추가
                .name(client.getUser().getName())
                .title(client.getTitle())
                .completedProject(client.getCompletedProject())
                .rating(client.getRating())
                .participateType(client.getParticipateType())
                .description(client.getUser().getDescription())
                .build();
    }

}
