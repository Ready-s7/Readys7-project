package com.example.readys7project.domain.review.repository;

import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.review.entity.QReview;
import com.example.readys7project.domain.review.entity.Review;
import com.example.readys7project.domain.review.enums.ReviewRole;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.example.readys7project.domain.user.developer.entity.QDeveloper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final QReview review = QReview.review;
    private final QDeveloper developer = QDeveloper.developer;
    private final QClient client = QClient.client;
    private final QProject project = QProject.project;

    // Alias conflict prevention
    private final QUser developerUser = new QUser("developerUser");
    private final QUser clientUser = new QUser("clientUser");

    // 클라이언트 목록 조회
    /**
     1. BooleanBuilder 생성. 즉 조회 조건이 들어오면 하나씩 붙이고, 없으면 건너뛰기.
     2. 클라이언트 고정 조건 추가. 왜냐면 어차피 클라이언트 조회 엔드 포인트라서 고정하기.
     3. 특정 평점 조건 추가.
     4. 최소 평점 이상 조건 추가.
     5. 최대 평점 이사 조건 추가.
     6. 실제 리뷰 목록 조회.

     가능한 클라이언트 조회 조건들
     1. 클라이언트 전체 리뷰
     2. 특정 평정만 조회
     3. 최소 평점 이상 조회
     4. 최대 평점 이하 조회
     5. 평점 범위 조회
     */
    @Override
    public Page<Review> searchReviewsByClient(
            Long clientId,
            Integer rating,
            Integer minRating,
            Integer maxRating,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(review.writerRole.eq(ReviewRole.DEVELOPER));

        builder.and(review.client.id.eq(clientId));

        if (rating != null) {
            builder.and(review.rating.eq(rating));
        }

        if (minRating != null) {
            builder.and(review.rating.goe(minRating));
        }

        if (maxRating != null) {
            builder.and(review.rating.loe(maxRating));
        }

        List<Review> content = queryFactory
                .selectFrom(review)
                .join(review.developer, developer).fetchJoin()
                .join(developer.user, developerUser).fetchJoin()
                .join(review.client, client).fetchJoin()
                .join(client.user, clientUser).fetchJoin()
                .join(review.project, project).fetchJoin()
                .where(builder)
                .orderBy(review.createdAt.desc(), review.id.desc())
                .offset(pageable.getOffset()) // 스탠다드 반..
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(review.count())
                .from(review)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);

    }

// 개발자 조회

    /**
     1. 조건을 담을 BooleanBuilder 를 만듬.
     2. 기본적으로 개발자 조회 이므로 고정한다.
     3. rating, minRating, maxRating 조건을 추가
     4. 조건으로 페이징 조회

     가능한 개발자 조회 조건 조건들
     1. 특정 개발자의 전체 리뷰 조회
     2. 특정 개발자의 특정 평점 리뷰 조회
     3. 특정 개발자의 최소 평점 이상 리뷰 조회
     4. 특정 개발자의 최대 평점 이하 이하 리뷰 조회
     5. 특적 개발자의 평점 범위 리뷰 조회
     */
    @Override
    public Page<Review> searchReviewsByDeveloper(
            Long developerId,
            Integer rating,
            Integer minRating,
            Integer maxRating,
            Pageable pageable
    ) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(review.developer.id.eq(developerId));
        builder.and(review.writerRole.eq(ReviewRole.CLIENT));

        if (rating != null) {
            builder.and(review.rating.eq(rating));
        }

        if (minRating != null) {
            builder.and(review.rating.goe(minRating));
        }

        if (maxRating != null) {
            builder.and(review.rating.loe(maxRating));
        }

        List<Review> content = queryFactory
                .selectFrom(review)
                .join(review.developer, developer).fetchJoin()
                .join(developer.user, developerUser).fetchJoin()
                .join(review.client, client).fetchJoin()
                .join(client.user, clientUser).fetchJoin()
                .join(review.project, project).fetchJoin()
                .where(builder)
                .orderBy(review.createdAt.desc(), review.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(review.count())
                .from(review)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }
}
