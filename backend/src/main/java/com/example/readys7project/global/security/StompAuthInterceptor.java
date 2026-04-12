package com.example.readys7project.global.security;

import com.example.readys7project.domain.user.auth.entity.User;
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

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class
        );

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
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
        }

        // 비정상 종료 처리
        if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            // Principal이 있을 때만 처리 (정상 연결 후 끊긴 경우)
            if (accessor.getUser() != null) {
                log.warn("STOMP DISCONNECT 감지: {}", accessor.getUser().getName());
                // 필요 시 추가 처리 (예: 접속 상태 관리)
            }
        }

        return message;
    }
}
