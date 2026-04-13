package com.example.readys7project.domain.user.client.service;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.dto.request.UpdateClientProfileRequestDto;
import com.example.readys7project.domain.user.client.dto.response.*;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ClientException;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    // 클라이언트 목록 조회
    public GetAllClientsListResponseDto getClients(
            CustomUserDetails customUserDetails,
            Pageable pageable
    ) {

        // 로그인 유저 정보 가져오기
        User user = customUserDetails.getUser();

        // Pageable 세팅
        Pageable convertPageable = PageRequest.of(
                pageable.getPageNumber() - 1,
                pageable.getPageSize());

        // 유저의 정보가 존재하는지 유저레포에 확인
        userRepository.findById(user.getId())
                .orElseThrow(() -> new ClientException(ErrorCode.USER_NOT_FOUND));

        // Client 페이징 조회
        Page<Client> clientPage = clientRepository.findAll(convertPageable);

        // clientPage를 ClientSummaryResponseDto로 변환
        List<ClientSummaryResponseDto> clientList = clientPage.getContent().stream()
                .map(ClientSummaryResponseDto::new)
                .toList();

        // GetAllClientsListResponseDto 반환
        return new GetAllClientsListResponseDto(
                clientList,
                clientPage.getNumber() + 1,
                clientPage.getSize(),
                clientPage.getTotalElements(),
                clientPage.getTotalPages()
        );
    }

    // 클라이언트 상세 조회
    @Transactional(readOnly = true)
    public GetClientDetailResponseDto getClientDetail(Long clientId, CustomUserDetails customUserDetails) {

        // 로그인 유저 정보 가져오기
        User user = customUserDetails.getUser();

        // 로그인 유저 권한 체크
        if(user.getUserRole() == null){
            throw new ClientException(ErrorCode.USER_UNAUTHORIZED);
        }

        // 클라이언트 레포에 존재여부 확인
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientException(ErrorCode.CLIENT_NOT_FOUND));

        // Dto 반환
        return new GetClientDetailResponseDto(
                client.getId(),
                client.getUser().getName(),
                client.getTitle(),
                client.getCompletedProject(),
                client.getRating(),
                client.getReviewCount(),
                client.getParticipateType(),
                client.getUser().getDescription()
        );
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

        // 본인 확인
        if(!client.getUser().getId().equals(user.getId())) {
            throw new ClientException(ErrorCode.USER_FORBIDDEN);
        }

        // 업데이트 메서드에 넘기기
        client.updateProfile(
                updateClientProfileRequestDto.title(),
                updateClientProfileRequestDto.participateType());

        clientRepository.saveAndFlush(client);

        return new UpdateClientProfileResponseDto(
                client.getId(),
                client.getTitle(),
                client.getParticipateType(),
                client.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public GetClientProjectsListResponseDto getMyProjects(CustomUserDetails customUserDetails, Pageable pageable) {

        // 로그인 유저 정보 가져오기
        User user = customUserDetails.getUser();

        // 페이징 세팅
        Pageable convertPageable = PageRequest.of(
                pageable.getPageNumber() - 1,
                pageable.getPageSize()
        );

        // 클라이언트 유저 찾아보기
        Client client = clientRepository.findByUser(user)
                .orElseThrow(() -> new ClientException(ErrorCode.CLIENT_NOT_FOUND));

        // ClientId로 프로젝트 페이징 조회
        Page<Project> projectPage = projectRepository.findByClientId(client.getId(), convertPageable);

        // Project를 ClientProjectSummaryResponseDto로 변환
        List<ClientProjectSummaryResponseDto> projectList = projectPage
                .map(project -> new ClientProjectSummaryResponseDto(
                        project.getId(),
                        project.getTitle(),
                        project.getStatus(),
                        project.getCurrentProposalCount(),
                        project.getCreatedAt()
                )).toList();

        // Dto 반환
        return new GetClientProjectsListResponseDto(
                projectList,
                pageable.getPageNumber() +1,
                pageable.getPageSize(),
                projectPage.getTotalElements(),
                projectPage.getTotalPages()
        );
    }
}
