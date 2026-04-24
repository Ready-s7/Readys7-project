package com.example.readys7project.domain.user.client.repository;

import com.example.readys7project.domain.user.client.entity.Client;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.example.readys7project.domain.user.auth.entity.QUser.user;
import static com.example.readys7project.domain.user.client.entity.QClient.client;

@RequiredArgsConstructor
public class ClientQueryRepositoryImpl implements ClientQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<Client> findAllWithPageable(Pageable pageable) {

        List<Client> content = jpaQueryFactory
                .selectFrom(client)
                .join(client.user, user).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(client.id.desc())
                .fetch();

        Long total = jpaQueryFactory
                .select(client.count())
                .from(client)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
