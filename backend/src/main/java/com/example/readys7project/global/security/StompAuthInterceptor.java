package com.example.readys7project.global.security;

import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.chat.chatroom.repository.ChatRoomRepository;
import com.example.readys7project.domain.chat.cs.entity.CsChatRoom;
import com.example.readys7project.domain.chat.cs.repository.CsChatRoomRepository;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.auth.enums.UserRole;
import com.example.readys7project.domain.user.auth.repository.UserRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.MessageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final CsChatRoomRepository csChatRoomRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                jwtTokenProvider.validateToken(token);
                String email = jwtTokenProvider.getEmail(token);

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new MessageException(ErrorCode.USER_NOT_FOUND));

                CustomUserDetails userDetails = new CustomUserDetails(user);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );

                accessor.setUser(authentication);
            }
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination != null) {
                // 일반 채팅방 권한 검증
                if (destination.startsWith("/receive/chat/rooms/")) {
                    validateChatRoomSubscription(accessor, destination);
                }
                // CS 채팅방 권한 검증
                else if (destination.startsWith("/receive/chat/cs/")) {
                    validateCsChatRoomSubscription(accessor, destination);
                }
            }
        }

        // 비정상 종료 처리
        if (StompCommand.DISCONNECT.equals(command)) {
            // Principal이 있을 때만 처리 (정상 연결 후 끊긴 경우)
            if (accessor.getUser() != null) {
                log.warn("STOMP DISCONNECT 감지: {}", accessor.getUser().getName());
            }
        }

        return message;
    }

    private void validateChatRoomSubscription(StompHeaderAccessor accessor, String destination) {
        Long roomId = extractRoomId(destination, "/receive/chat/rooms/");
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();

        if (auth == null) {
            throw new MessageException(ErrorCode.USER_UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MessageException(ErrorCode.CHATROOM_NOT_FOUND));

        boolean isClient = chatRoom.getClient().getUser().getId().equals(user.getId());
        boolean isDeveloper = chatRoom.getDeveloper().getUser().getId().equals(user.getId());

        if (!isClient && !isDeveloper) {
            throw new MessageException(ErrorCode.USER_FORBIDDEN);
        }
    }

    private void validateCsChatRoomSubscription(StompHeaderAccessor accessor, String destination) {
        Long roomId = extractRoomId(destination, "/receive/chat/cs/");
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();

        if (auth == null) {
            throw new MessageException(ErrorCode.USER_UNAUTHORIZED);
        }

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        CsChatRoom room = csChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new MessageException(ErrorCode.CHATROOM_NOT_FOUND));

        boolean isInquirer = room.getInquirer().getId().equals(user.getId());
        boolean isAdmin = user.getUserRole() == UserRole.ADMIN;

        if (!isInquirer && !isAdmin) {
            throw new MessageException(ErrorCode.USER_FORBIDDEN);
        }
    }

    private Long extractRoomId(String destination, String prefix) {
        try {
            return Long.parseLong(destination.substring(prefix.length()));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            throw new MessageException(ErrorCode.INVALID_INPUT);
        }
    }
}
