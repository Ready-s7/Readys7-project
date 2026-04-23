package com.example.readys7project.domain.chat.chatroom.service;

import com.example.readys7project.domain.chat.chatroom.dto.request.CreateChatRoomRequestDto;
import com.example.readys7project.domain.chat.chatroom.dto.response.ChatRoomResponseDto;
import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.proposal.entity.Proposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.proposal.repository.ProposalRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.repository.DeveloperRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.ChatRoomException;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatRoomServiceTest {

    @InjectMocks
    private ChatRoomService chatRoomService;

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private DeveloperRepository developerRepository;
    @Mock private ProposalRepository proposalRepository;

    private User createTestUser(Long id, String email, UserRole role) {
        User u = User.builder().email(email).name("User"+id).userRole(role).build();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    @DisplayName("성공: 채팅방 생성")
    void createChatRoom_Success() {
        String email = "c@t.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        Client client = Client.builder().user(user).build();
        ReflectionTestUtils.setField(client, "id", 10L);
        Project project = Project.builder().client(client).title("T").build();
        ReflectionTestUtils.setField(project, "id", 1L);
        Developer dev = Developer.builder().user(createTestUser(2L, "d@t.com", UserRole.DEVELOPER)).build();
        ReflectionTestUtils.setField(dev, "id", 20L);
        Proposal proposal = Proposal.builder().status(ProposalStatus.ACCEPTED).build();

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(clientRepository.findByUser(user)).willReturn(Optional.of(client));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(developerRepository.findById(20L)).willReturn(Optional.of(dev));
        given(chatRoomRepository.existsByProjectAndClientAndDeveloper(any(), any(), any())).willReturn(false);
        given(proposalRepository.findByProjectAndDeveloper(any(), any())).willReturn(Optional.of(proposal));
        
        ChatRoom cr = ChatRoom.builder().project(project).client(client).developer(dev).build();
        ReflectionTestUtils.setField(cr, "id", 100L);
        given(chatRoomRepository.save(any())).willReturn(cr);

        ChatRoomResponseDto res = chatRoomService.createChatRoom(new CreateChatRoomRequestDto(1L, 20L), email);
        assertThat(res.id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("실패: 이미 존재하는 채팅방")
    void createChatRoom_AlreadyExists_Fail() {
        String email = "c@t.com";
        User user = createTestUser(1L, email, UserRole.CLIENT);
        Client client = Client.builder().user(user).build();
        ReflectionTestUtils.setField(client, "id", 10L);
        Project project = Project.builder().client(client).build();
        ReflectionTestUtils.setField(project, "id", 1L);
        Developer dev = Developer.builder().user(createTestUser(2L, "d@t.com", UserRole.DEVELOPER)).build();
        ReflectionTestUtils.setField(dev, "id", 20L);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(clientRepository.findByUser(user)).willReturn(Optional.of(client));
        given(projectRepository.findById(1L)).willReturn(Optional.of(project));
        given(developerRepository.findById(20L)).willReturn(Optional.of(dev));
        given(chatRoomRepository.existsByProjectAndClientAndDeveloper(any(), any(), any())).willReturn(true);

        assertThatThrownBy(() -> chatRoomService.createChatRoom(new CreateChatRoomRequestDto(1L, 20L), email))
                .isInstanceOf(ChatRoomException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.CHATROOM_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("성공: 내 채팅방 목록 조회")
    void getMyChatRooms_Success() {
        User user = createTestUser(1L, "u@t.com", UserRole.CLIENT);
        PageRequest pr = PageRequest.of(0, 10);
        ChatRoom cr = mock(ChatRoom.class);
        given(cr.getProject()).willReturn(Project.builder().title("T").build());
        given(cr.getClient()).willReturn(mock(Client.class));
        given(cr.getClient().getUser()).willReturn(user);
        given(cr.getDeveloper()).willReturn(mock(Developer.class));
        given(cr.getDeveloper().getUser()).willReturn(mock(User.class));

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(chatRoomRepository.findMyChatRooms(eq(user), any())).willReturn(new PageImpl<>(List.of(cr), pr, 1));

        Page<ChatRoomResponseDto> res = chatRoomService.getMyChatRooms("u@t.com", pr);
        assertThat(res.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("실패: 삭제 권한 없음")
    void deleteChatRoom_Forbidden_Fail() {
        User user = createTestUser(1L, "u@t.com", UserRole.CLIENT);
        ChatRoom cr = mock(ChatRoom.class);
        given(cr.getClient()).willReturn(mock(Client.class));
        given(cr.getClient().getUser()).willReturn(createTestUser(2L, "o@t.com", UserRole.CLIENT));
        given(cr.getDeveloper()).willReturn(mock(Developer.class));
        given(cr.getDeveloper().getUser()).willReturn(createTestUser(3L, "d@t.com", UserRole.DEVELOPER));

        given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
        given(chatRoomRepository.findById(1L)).willReturn(Optional.of(cr));

        assertThatThrownBy(() -> chatRoomService.deleteChatRoom(1L, "u@t.com"))
                .isInstanceOf(ChatRoomException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_FORBIDDEN);
    }
}
