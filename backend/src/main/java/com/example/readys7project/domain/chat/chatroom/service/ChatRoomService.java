package com.example.readys7project.domain.chat.chatroom.service;

import com.example.readys7project.domain.chat.chatroom.dto.ChatRoomDto;
import com.example.readys7project.domain.chat.chatroom.dto.request.CreateChatRoomRequestDto;
import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.example.readys7project.domain.chat.message.service.UnreadCountService;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;import com.example.readys7project.global.exception.domain.ChatRoomException;import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;
    private final ProposalRepository proposalRepository;
    private final UnreadCountService unreadCountService;

    @Transactional
    public ChatRoomDto createChatRoom(CreateChatRoomRequestDto request, String email) {

        // user 가져오기
        User user = findUser(email);

        // 클라이언트인지 검증
        if (!user.getUserRole().equals(UserRole.CLIENT)) {
            throw new ChatRoomException(ErrorCode.USER_FORBIDDEN);
        }

        // client 가져오기
        Client client = clientRepository.findByUser(user).orElseThrow(
                () -> new ChatRoomException(ErrorCode.CLIENT_NOT_FOUND)
        );

        // project 가져오기
        Project project = projectRepository.findById(request.projectId()).orElseThrow(
                () -> new ChatRoomException(ErrorCode.PROJECT_NOT_FOUND)
        );

        // 해당 프로젝트의 발안자가 채팅방 생성자인지 검증
        if (!client.getId().equals(project.getClient().getId())) {
            throw new ChatRoomException(ErrorCode.USER_FORBIDDEN);
        }

        // developer 가져오기
        Developer developer = developerRepository.findById(request.developerId()).orElseThrow(
                () -> new ChatRoomException(ErrorCode.DEVELOPER_NOT_FOUND)
        );

        // 이미 해당 채팅방이 있는 지 검증
        if (chatRoomRepository.existsByProjectAndClientAndDeveloper(project, client, developer)) {
            throw new ChatRoomException(ErrorCode.CHATROOM_ALREADY_EXISTS);
        }

        // project와 developer로 제안서 가져오기
        Proposal proposal = proposalRepository.findByProjectAndDeveloper(project, developer).orElseThrow(
                () -> new ChatRoomException(ErrorCode.PROPOSAL_NOT_FOUND)
        );

        // 해당 제안서가 승인 상태인지 검증
        if (!proposal.getStatus().equals(ProposalStatus.ACCEPTED)) {
            throw new ChatRoomException(ErrorCode.PROPOSAL_NOT_ACCEPTED);
        }

        // ChatRoom 객체 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .project(project)
                .client(client)
                .developer(developer)
                .build();

        // ChatRoom 객체 저장
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        return convertToDto(savedChatRoom, user.getId());
    }

    @Transactional(readOnly = true)
    public Page<ChatRoomDto> getMyChatRooms(String email, Pageable pageable) {
        // user 가져오기
        User user = findUser(email);

        // chatRoom 페이지 조회
        // 메서드 내부에서 client인지 developer인지 검증
        return chatRoomRepository.findMyChatRooms(user, pageable)
                .map(chatRoom -> convertToDto(chatRoom, user.getId()));
    }

    @Transactional
    public void deleteChatRoom(Long roomId, String email) {
        User user = findUser(email);
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(
                () -> new ChatRoomException(ErrorCode.CHATROOM_NOT_FOUND)
        );

        // 본인의 채팅방인지 검증 (Client 또는 Developer)
        boolean isClient = chatRoom.getClient().getUser().getId().equals(user.getId());
        boolean isDeveloper = chatRoom.getDeveloper().getUser().getId().equals(user.getId());

        if (!isClient && !isDeveloper) {
            throw new ChatRoomException(ErrorCode.USER_FORBIDDEN);
        }

        chatRoomRepository.delete(chatRoom);
    }

    public ChatRoomDto convertToDto(ChatRoom chatRoom, Long userId) {
        Long unreadCount = unreadCountService.getCount(chatRoom.getId(), userId);

        return ChatRoomDto.builder()
                .id(chatRoom.getId())
                .projectId(chatRoom.getProject().getId())
                .projectTitle(chatRoom.getProject().getTitle())
                .clientId(chatRoom.getClient().getId())
                .clientName(chatRoom.getClient().getUser().getName())
                .developerId(chatRoom.getDeveloper().getId())
                .developerName(chatRoom.getDeveloper().getUser().getName())
                .unreadCount(unreadCount)
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ChatRoomException(ErrorCode.USER_NOT_FOUND)
        );
    }
}
