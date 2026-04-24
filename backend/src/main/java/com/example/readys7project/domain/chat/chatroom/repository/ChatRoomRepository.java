package com.example.readys7project.domain.chat.chatroom.repository;

import com.example.readys7project.domain.chat.chatroom.entity.ChatRoom;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.developer.entity.Developer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>, ChatRoomQueryRepository {
    Boolean existsByProjectAndClientAndDeveloper(Project project, Client client, Developer developer);
}
