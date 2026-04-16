package com.example.readys7project.domain.chat.cs.repository;

import com.example.readys7project.domain.chat.cs.entity.CsChatRoom;
import com.example.readys7project.domain.chat.cs.entity.CsMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CsMessageRepository extends JpaRepository<CsMessage, Long> {
    List<CsMessage> findByCsChatRoomOrderByCreatedAtAsc(CsChatRoom csChatRoom);
    Page<CsMessage> findByCsChatRoom(CsChatRoom csChatRoom, Pageable pageable);
}
