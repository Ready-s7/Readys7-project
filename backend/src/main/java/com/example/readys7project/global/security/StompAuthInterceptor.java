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
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        StompCommand command = accessor.getCommand();
        String token = accessor.getFirstNativeHeader("Authorization");
        
        // 모든 프레임에 대해 토큰이 있다면 인증 주입
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String pureToken = token.substring(7);
                jwtTokenProvider.validateToken(pureToken);
                String email = jwtTokenProvider.getEmail(pureToken);
                User user = userRepository.findByEmail(email).orElseThrow(() -> new MessageException(ErrorCode.USER_NOT_FOUND));
                
                CustomUserDetails userDetails = new CustomUserDetails(user);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                accessor.setUser(auth);
            } catch (Exception e) {
                log.error("[STOMP] Auth Fail [{}]: {}", command, e.getMessage());
            }
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            validateSubscription(accessor);
        }

        return message;
    }

    private void validateSubscription(StompHeaderAccessor accessor) {
        String dest = accessor.getDestination();
        if (dest == null) return;
        
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
        if (auth == null) throw new MessageException(ErrorCode.USER_UNAUTHORIZED);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userDetails.getUser();

        if (dest.startsWith("/receive/chat/rooms/")) {
            Long roomId = Long.parseLong(dest.substring("/receive/chat/rooms/".length()));
            ChatRoom room = chatRoomRepository.findById(roomId).orElseThrow(() -> new MessageException(ErrorCode.CHATROOM_NOT_FOUND));
            if (!room.getClient().getUser().getId().equals(user.getId()) && !room.getDeveloper().getUser().getId().equals(user.getId())) {
                throw new MessageException(ErrorCode.USER_FORBIDDEN);
            }
        }
    }
}
