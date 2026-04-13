package com.example.readys7project.domain.chat.message.repository;

import com.example.readys7project.domain.chat.message.entity.Message;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessageQueryRepository {
    List<Message> findMessages(Long roomId, Long lastMessageId, Pageable pageable);
}
