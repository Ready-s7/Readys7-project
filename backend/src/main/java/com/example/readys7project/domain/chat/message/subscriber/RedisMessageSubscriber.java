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

import com.example.readys7project.domain.chat.cs.dto.CsMessageResponseDto;

@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper redisObjectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            byte[] body = message.getBody();

            if (channel.startsWith("chat-room:")) {
                String roomId = channel.replace("chat-room:", "");
                MessageResponseDto responseDto = redisObjectMapper.readValue(body, MessageResponseDto.class);
                if (responseDto != null) {
                    messagingTemplate.convertAndSend("/receive/chat/rooms/" + roomId, responseDto);
                }
            } else if (channel.startsWith("cs-room:")) {
                String roomId = channel.replace("cs-room:", "");
                CsMessageResponseDto responseDto = redisObjectMapper.readValue(body, CsMessageResponseDto.class);
                if (responseDto != null) {
                    messagingTemplate.convertAndSend("/receive/chat/cs/" + roomId, responseDto);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new MessageException(ErrorCode.MESSAGE_PUBLISH_FAILED);
        }
    }
}
