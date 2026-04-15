package com.example.readys7project.domain.chat.message.subscriber;

import com.example.readys7project.domain.chat.message.dto.response.MessageResponseDto;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.MessageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper redisObjectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Redis에서 받은 메시지를 DTO로 역직렬화
            MessageResponseDto responseDto = redisObjectMapper.readValue(
                    message.getBody(), MessageResponseDto.class
            );

            if (responseDto == null) {
                return;
            }

            // 채널명에서 roomId 추출 (chat-room:{roomId})
            String channel = new String(message.getChannel());
            String roomId = channel.replace("chat-room:", "");

            // STOMP 구독자들에게 브로드캐스트
            messagingTemplate.convertAndSend("/receive/chat/rooms/" + roomId, responseDto);

        } catch (Exception e) {
            e.printStackTrace();
            throw new MessageException(ErrorCode.MESSAGE_PUBLISH_FAILED);
        }
    }
}
