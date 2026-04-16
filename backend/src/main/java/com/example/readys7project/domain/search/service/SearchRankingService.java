package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.search.Repository.SearchRankingRepository;
import com.example.readys7project.domain.search.dto.response.*;
import com.example.readys7project.domain.search.entity.SearchRanking;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchRankingService {

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final SearchRankingRepository searchRankingRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RANKING_KEY = "ranking";

    /* 어떤 사용자가 악의적으로 수만자의 긴 텍스트를 검색창에 넣으면 서버 메모리가 감당을 못할 수 있음,
       이러한 상황을 방지하기 위한 방어코드*/
    private static final int MAX_KEYWORD = 20;

    // 랭킹 업데이트를 위해서 readOnly 제거
    @Transactional
    public TotalSearchResponseDto getTotalSearchV1(String keyword, Pageable pageable) {

        // 검색값 (keyword)가 빈값인지 체크
        if (keyword == null || keyword.isBlank()) {
            // NPE 방지를 위해 빈 객체를 리턴
            return empty(pageable);
        }
        // 양 끝 공백 제거
        String trimKeyword = keyword.trim();

        // 의미 없는 검색 방지 (ex 1글자 검색)
        if (trimKeyword.length() < 2) {
            return empty(pageable);
        }

        // 최대 검색 길이를 초과한 경우 예외 던지기
        if (trimKeyword.length() > MAX_KEYWORD) {
            throw new SearchException(ErrorCode.SEARCH_LENGTH_TOO_LONG);
        }

        updateRankingCount(trimKeyword);

        try {
            Page<ProjectsTotalSearchResponseDto> projectPage = projectRepository.projectsTotalSearch(trimKeyword, pageable);
            Page<CategoriesTotalSearchResponseDto> categoryPage = categoryRepository.categoriesTotalSearch(trimKeyword, pageable);
            Page<SkillsTotalSearchResponseDto> skillPage = skillRepository.skillsTotalSearch(trimKeyword, pageable);

            return new TotalSearchResponseDto(projectPage, categoryPage, skillPage);
        } catch (Exception e) {
            log.warn("통합 검색 중 DB 에러 발생 - 키워드: [{}], 메시지: {}", trimKeyword, e.getMessage());
            throw new SearchException(ErrorCode.SEARCH_FAILED);
        }
    }

    // 상위 10개 인기 검색어 순위 조회

    // Set<ZSetOperations.TypedTuple<String>> -> Redis에서 꺼내온 [ 검색어 + 점수 ] 세트들의 묶음
    // String -> 실제 데이터, TypedTuple -> Redis ZSet의 핵심인 [ 값(Value) + 점수(Score)]를 한번에 담고 있는 상자
    // getValue()를 호출하면 -> "Java"가 나오고, getScore() -> "100.0"이 나옴
    // Set<> -> ZSet은 중복을 허용하지 않기 때문에 Set 인터페이스로 사용

    public List<PopularRankingResponseDto> getPopularRanking(int limit) {
        Set<ZSetOperations.TypedTuple<Object>> rankingList =
                redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_KEY, 0, limit - 1);

        // reverseRangeWithScores -> 순위만 가져오지 말고, 점수까지고 같이 달라는 의미

        if (rankingList == null || rankingList.isEmpty()) {
            return Collections.emptyList();
        }

        AtomicInteger rank = new AtomicInteger(1);

        return rankingList.stream()
                .map(tuple -> {
                    String rawValue = String.valueOf(tuple.getValue());
                    String cleanKeyword = rawValue.replace("\"", "");
                    return PopularRankingResponseDto.builder()
                            .ranking(rank.getAndIncrement()) // 순위만 할당
                            .keyword(cleanKeyword)
                            .build();
                }).toList();

        /* AtomicInteger -> 자바에서 멀티스레드 환경에서도 안전하게 숫자를 계산할 수 있게해주는 원자적 변수,
         여러 사람이 동시에 접근해도 숫자가 꼬이지 않도록 특수 설계된 전용 계산기
         AtomicInteger rank = new AtomicInteger(1);
         -> 정수값을 가진 변수를 만드는데, 초기값을 1로 설정하겠다는 의미
         AtomicInteger를 사용하는 이유 : 자바의 람다 안에서는 지역변수를 수정할 수 없는 규칙이 있다,
         하지만 AtomicInteger를는 객체이기 때문에, 람다 안에서도 내부 값을 안전하게 바꿀 수 있다*/

        // getAndIncrement() -> "현재 값을 먼저 가져오고, 그 다음에 1을 증가시켜라는 의미
    }

    private void updateRankingCount(String keyword) {
        // DB에 점수 업데이트하는 로직
        SearchRanking searchRanking = searchRankingRepository.findByKeywordAndIsDeletedFalse(keyword)
                .orElseGet( () -> new SearchRanking(keyword));
        // orElseGet -> 없으면 뒤에 명령문을 실행하라는 의미

        if (searchRanking.getId() != null) {
            // 이미 검색했던 값이면 +1
            searchRanking.increaseSearchCount();
        } else {
            // 처음 생긴 검색한 값이면 DB에 저장
            searchRankingRepository.save(searchRanking);
        }

        // Redis ZSet 점수 업데이트 로직
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, keyword, 1);
    }


    // 빈 값을 리턴해주는 공용 정적 팩토리 메서드
    public static TotalSearchResponseDto empty(Pageable pageable) {
        return new TotalSearchResponseDto(
                Page.empty(pageable),
                Page.empty(pageable),
                Page.empty(pageable)
        );
    }

}

/* Redis는 데이터를 어떻게 전달해줄까?
 Redis의 ZSet은 내부적으로 [값(Value), 점수(Score)]가 묶인 상태로 저장되지만, 우리가 자바에서 꺼낼 때는
 어떤 메서드를 쓰느냐에 따라 데이터의 형태가 달라짐
 1. reverseRange를 썼을 때 -> 점수 빼고 이름만 보냄 Set<String>을 줌
 -> 결과 : ["Java", "Spring", "Redis"]
 2. reverseRangeWithScores를 썻을 때 -> 이름이랑 점수를 세트로 보내줌
 -> 결과 : [(Java, 100.0), (Spring, 110.0), (Redis, 150.0)]
 reverseRangeWithScores을 사용하면 좋은점이 단순히 이름만 가져오는게 아닌, 몇 번 검색이 됐는지도 확인할 수 있음*/

/* 검색의 전체적인 흐름

 1. 검색 발생 (저장)
 사용자가 검색창에 Java를 입력하고 엔터를 치면 getTotalSearchV1이 실행됨

 2. 점수 업데이트 (Write)
 updateRankingCount 로직이 돌아가며 두 군데에 신호를 보냄,
 MySQL -> Java 검색 횟수 1 증가시켜 (영구 보존용)
 Redis (ZSet) -> Java가 이미 있으면 +1, 없으면 새로 만들고 점수 1 부여해줘 -> 이때 Redis는 실시간으로 순위를 재계산함

 3. 순위 요청 (Read)
 사용자가 메인 페이지에서 접속하거나 인기 검색어 버튼을 누르면 getPopularRanking이 호출됨

 4. 데이터 가공
 Redis에서 상위 10개를 Set<TypedTuple<String>> 형태로 받아옴,
 4.1 순수 키워드 추출 -> getValue()로 "Java"만 쏙 뽑아옴
 4.2 따옴표 청소 -> JSON 직렬화로 생긴 ""를 replace로 제거
 4.3 순위 매기기 -> AtomicInteger 가 1,2,3... 번호를 붙여줌

 5. 최종 응답
 PopularRankingResponseDto 리스트가 JSON 형태로 사용자 화면에 뿌려짐*/
