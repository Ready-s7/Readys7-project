package com.example.readys7project.domain.user.developer.repository;

import com.example.readys7project.domain.category.entity.QCategory;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.project.entity.QProject;
import com.example.readys7project.domain.proposal.entity.QProposal;
import com.example.readys7project.domain.proposal.enums.ProposalStatus;
import com.example.readys7project.domain.search.dto.response.DeveloperGlobalSearchResponseDto;
import com.example.readys7project.domain.user.auth.entity.QUser;
import com.example.readys7project.domain.user.client.entity.QClient;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.user.developer.entity.QDeveloper;
import com.example.readys7project.domain.user.enums.ParticipateType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.types.dsl.Expressions;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeveloperQueryRepositoryImpl implements DeveloperQueryRepository {

    private final JPAQueryFactory queryFactory;
    QDeveloper qDeveloper = QDeveloper.developer;

    // 전체 개발자 목록 조회
    @Override
    public Page<Developer> findAllWithUser(Pageable pageable) {
        QUser qUser = QUser.user;

        // 실제 데이터 조회
        List<Developer> content = queryFactory
                .selectFrom(qDeveloper)
                .join(qDeveloper.user, qUser).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qDeveloper.count())
                .from(qDeveloper)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 개발자 검색 (skill, minRating)
    @Override
    public Page<Developer> searchDevelopers(List<String> skills, Double minRating, Pageable pageable) {
        QUser qUser = QUser.user;

        // 동적 조건 생성 (null이면 조건 무시)
        // 조건이 있을 때만 동적으로 where절에 추가하는 QueryDSL 도구
        BooleanBuilder builder = new BooleanBuilder();

        if (skills != null && !skills.isEmpty()) {
            BooleanBuilder skillBuilder = new BooleanBuilder();
            for (String s : skills) {
                if (s != null && !s.isBlank()) {
                    skillBuilder.or(
                            // [요구사항 반영]
                            // 1. skills 필드는 @Convert로 JSON 컬럼에 저장되므로 QueryDSL contains() 사용 불가
                            // 2. MySQL JSON_CONTAINS 함수로 JSO 배열 내 정확한 값 일치 여부 검사
                            // 3. UPPER()를 통해 대소문자 구분 없이 검색 가능.
                            // 4. ex) "java", "JAVA", "Java" 모두 "JAVA" 로 조회 가능
                            // 5. ex) "java" 검색 시 "JavaScript"는 조회되지 않음. (정확한 일치)
                            Expressions.booleanTemplate(
                                    "function('json_contains', UPPER({0}), function('json_quote', {1})) = 1",
                                    qDeveloper.skills,
                                    s.toUpperCase()                      // Java단에서 미리 대문자로 변환
                            )
                    );
                }
            }
            builder.and(skillBuilder);                         // 생성된 OR 조건 뭉치를 메인 빌더에 추가
        }
        if (minRating != null) {
            builder.and(qDeveloper.rating.goe(minRating));    // rating >= minRating
        }

        // 실제 데이터 조회
        List<Developer> content = queryFactory
                .selectFrom(qDeveloper)
                .join(qDeveloper.user, qUser).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 개수 조회
        Long total = queryFactory
                .select(qDeveloper.count())
                .from(qDeveloper)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 내 프로젝트 목록 조회
    @Override
    public Page<Project> findMyProjects(Developer developer, Pageable pageable) {
        QProposal qProposal = QProposal.proposal;
        QProject qProject = QProject.project;
        QClient qClient = QClient.client;
        QUser qUser = QUser.user;
        QCategory qCategory = QCategory.category;

        // [fetchJoin()이 아닌 Join()으로 한 이유]
        // fetchJoin()은 select/from의 주인에 딸린 연관관계만 가능. select 대상이 proposal이 아닌 project이므로
        // fetchJoin() 사용 시 owner(Proposal)가 select에 없어 SemanticException 발생.
        // fetchJoin() -> join()으로 변경하여 해결
        List<Project> content = queryFactory
                .select(qProposal.project)
                .from(qProposal)
                .join(qProposal.project, qProject)
                .join(qProject.client, qClient)
                .join(qClient.user, qUser)
                .join(qProject.category, qCategory)
                .where(
                        qProposal.developer.eq(developer)                      // 본인 제안서
                                .and(qProposal.status.eq(ProposalStatus.ACCEPTED))     // 상태가 ACCEPTED인 것만
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(qProposal.count())
                .from(qProposal)
                .where(
                        qProposal.developer.eq(developer)
                                .and(qProposal.status.eq(ProposalStatus.ACCEPTED))
                ).fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public Page<DeveloperGlobalSearchResponseDto> developerGlobalSearch(String keyword, Pageable pageable) {

        QDeveloper qDeveloper = QDeveloper.developer;

        List<DeveloperGlobalSearchResponseDto> content = queryFactory
                .select(Projections.constructor(DeveloperGlobalSearchResponseDto.class,
                        qDeveloper.id,
                        qDeveloper.user.id,
                        qDeveloper.user.name,
                        qDeveloper.title,
                        qDeveloper.rating,
                        qDeveloper.reviewCount,
                        qDeveloper.completedProjects,
                        qDeveloper.skills,
                        qDeveloper.minHourlyPay,
                        qDeveloper.maxHourlyPay,
                        qDeveloper.responseTime,
                        qDeveloper.user.description,
                        qDeveloper.availableForWork,
                        qDeveloper.participateType,
                        qDeveloper.createdAt,
                        qDeveloper.updatedAt))
                .from(qDeveloper)
                .leftJoin(qDeveloper.user)
                .where(searchAllCondition(keyword))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qDeveloper.createdAt.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(qDeveloper.count())
                .from(qDeveloper)
                .where(searchAllCondition(keyword));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

    private Predicate searchAllCondition(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()){
            return null;
        }

        String trimmedKeyword = keyword.trim();

        BooleanBuilder builder = new BooleanBuilder();

        builder.or(titleLike(trimmedKeyword));
        builder.or(descriptionLike(trimmedKeyword));
        builder.or(nameLike(trimmedKeyword));
        builder.or(skillLike(trimmedKeyword));
        builder.or(minHourlyPayLike(trimmedKeyword));
        builder.or(maxHourlyPayLike(trimmedKeyword));
        builder.or(participateTypeLike(trimmedKeyword));

        return builder.getValue();
    }

    private BooleanExpression titleLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        return qDeveloper.title.containsIgnoreCase(keyword);
    }

    private BooleanExpression descriptionLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return qDeveloper.user.description.containsIgnoreCase(keyword);
    }

    private BooleanExpression nameLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return qDeveloper.user.name.containsIgnoreCase(keyword);
    }

    private BooleanExpression skillLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return Expressions.booleanTemplate(
                "CAST(JSON_CONTAINS({0}, JSON_QUOTE({1})) AS integer) = 1",
                qDeveloper.skills,
                keyword
        );
    }

    private BooleanExpression minHourlyPayLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        try {
            Integer value = Integer.parseInt(keyword);
            return qDeveloper.minHourlyPay.goe(value);
        } catch (NumberFormatException e){
            return null;
        }
    }

    private BooleanExpression maxHourlyPayLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        try {
            Integer value = Integer.parseInt(keyword);
            return qDeveloper.maxHourlyPay.loe(value);
        } catch (NumberFormatException e){
            return null;
        }
    }

    private BooleanExpression participateTypeLike(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        try {
            ParticipateType participateType = ParticipateType.valueOf(keyword.toUpperCase());
            return qDeveloper.participateType.eq(participateType);
        } catch (IllegalArgumentException e){
            return null;
        }
    }


}

