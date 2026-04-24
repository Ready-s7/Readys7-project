package com.example.readys7project.domain.chat.chatroom.repository;

import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.user.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

public interface ChatRoomQueryRepository {
    Page<ChatRoom> findMyChatRooms(User user, Pageable pageable);
}