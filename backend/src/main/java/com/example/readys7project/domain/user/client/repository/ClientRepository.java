package com.example.readys7project.domain.user.client.repository;

import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.domain.user.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> , ClientCustomRepository{
    Optional<Client> findByUser(User user);

}
