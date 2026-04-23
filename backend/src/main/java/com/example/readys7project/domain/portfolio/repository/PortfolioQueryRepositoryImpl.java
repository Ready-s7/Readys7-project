package com.example.readys7project.domain.portfolio.repository;


import com.example.readys7project.domain.portfolio.entity.Portfolio;
import com.example.readys7project.domain.portfolio.entity.QPortfolio;
import com.example.readys7project.domain.user.developer.entity.QDeveloper;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PortfolioQueryRepositoryImpl implements PortfolioQueryRepository  {

    private final JPAQueryFactory queryFactory;
    private final QPortfolio portfolio = QPortfolio.portfolio;

    // 특정 개발자 포트폴리오 조회
    /**
     로직 흐름.
     1. 특정 개발자의 포트폴리오만 대상잡기.
     2. 선택적으로 skill이 조건으로 들어오면 그 스킬을 포함한 포트폴리오만 필터링.
     3. 결과는 최신순으로 정렬.
     4. 반환.

     개발자 포트폴리오 조건들
     1. 특정 개발자의 전체 포트폴리오 조회
     2. 특정 개발자의 특정 스킬 포함 포트폴리오 조회
     3. 최신순 페이지 조회
     */
    @Override
    public Page<Portfolio> searchPortfolios(Long developerId,String skill, Pageable pageable) {


        BooleanBuilder builder = new BooleanBuilder();


        // 무조건 개발자의 입력을 받아야한다.
        builder.and(portfolio.developer.id.eq(developerId));

//        // skills json 칼럼 안에, 사용자가 요청한 skill이 포함되어 있는지 검사위해 사용
//        if (skill != null && !skill.isBlank()) {
//            // 특정 개발자의 포트폴리오
//            // 특정 개발자 + 특정 스킬
//            builder.and(
//                    // 지금 skills는 json 칼럼이다.
//                    // 즉, DB안에는 ["Java","Spring","AWS"] 이런식으로 존재.
//                    // 따라서 쿼리 dsl은 사용 불가.
//                    // 따라서 booleanTemplate을 활용해서 템플릿 문자열로 작성한다.
//                    // 즉, Json을 쓰기 위한 방법이다.
//                    Expressions.booleanTemplate(
//                            // 심화..
//                            // 첫번째 {0}인자는 DB 컬럼인 portfolio.skills 를 받는다.
//                            // 두번째 {1}인자는 사용자가 검색 조건으로 넣은 skill 값
//                            // 여기서 0, 1은 하드 코딩이 아니라/ {0} = 첫 번째 인자, {1} = 두 번째 인자/ 를 가르키는 문법이다.
//                            // 즉, 숫자를 넣는게 아니라, 쿼리 dsl이 몇 번째 인자를 어디에 넣을지 알려주는 역할이다.
//                            // JSON_CONTAINS, JSON_QUOTE 은 Mysql 함수이다.
//                            // JSON_CONTAINS은 JSON 안에 특정 값이 포함되어 있는지 검사하는 함수이다.
//                            // JSON_QUOTE은 일반 문자열을 JSON 문자열 형식으로 바꿔주는 함수이다.
//                            // 즉, JSON_CONTAINS이  포함되어 있으면 true, 아니면 false 로 동작한다.
//                            "JSON_CONTAINS({0}, JSON_QUOTE({1}))",
//                            portfolio.skills, // {0} 자리에 들어감
//                            skill             // {1} 자리에 들어감
//                    )
//            );
//        }


        if (skill != null && !skill.isBlank()) {
            builder.and(
                    Expressions.booleanTemplate(
                            "function('json_contains', {0}, function('json_quote', {1})) = 1",
                            portfolio.skills,
                            skill
                    )
            );
        }

        List<Portfolio> content = queryFactory
                .selectFrom(portfolio)
                .join(portfolio.developer, QDeveloper.developer).fetchJoin()
                .where(builder)
                .orderBy(portfolio.createdAt.desc(), portfolio.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(portfolio.count())
                .from(portfolio)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }


    // 특정 개발자가 아닌. 전체 포트폴리오 조회가 가능하다.
    @Override
    public Page<Portfolio> searchAllPortfolios(String skill, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        if (skill != null && !skill.isBlank()) {
            builder.and(
                    Expressions.booleanTemplate(
                            "function('json_contains', {0}, function('json_quote', {1})) = 1",
                            portfolio.skills,
                            skill
                    )
            );
        }

        List<Portfolio> content = queryFactory
                .selectFrom(portfolio)
                .where(builder)
                .orderBy(portfolio.createdAt.desc(), portfolio.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(portfolio.count())
                .from(portfolio)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }


}
