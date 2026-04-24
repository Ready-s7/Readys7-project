package com.example.readys7project.domain.chat.cs.repository;

import com.example.readys7project.domain.chat.cs.entity.CsChatRoom;
import com.example.readys7project.domain.chat.cs.enums.CsStatus;
import com.example.readys7project.domain.user.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsChatRoomRepository extends JpaRepository<CsChatRoom, Long> {
    Page<CsChatRoom> findByInquirer(User inquirer, Pageable pageable);
    Page<CsChatRoom> findByStatus(CsStatus status, Pageable pageable);
}
