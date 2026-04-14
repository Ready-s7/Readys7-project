package com.example.readys7project.domain.user.client.repository;

import com.example.readys7project.domain.user.client.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientCustomRepository{

    Page<Client> findAllWithPageable(Pageable pageable);
}
