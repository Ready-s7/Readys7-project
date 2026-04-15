package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.project.repository.ProjectRepository;
import com.example.readys7project.domain.search.dto.response.CategoryPopularSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.PopularSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.ProjectPopularSearchResponseDto;
import com.example.readys7project.domain.search.dto.response.SkillPopularSearchResponseDto;
import com.example.readys7project.domain.skill.repository.SkillRepository;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;

    /* ConcurrentHashMap -> 일반적인 HashMap은 여러 사람이 동시에 값을 수정하면 데이터가 꼬이거나 에러가 날 수 있는데,
       Concurrent가 붙은 HashMap은 멀티스레드 환경에서도 안전하게 데이터를 저장하도록 해준다.*/
    private final Map<String, Integer> ranking = new ConcurrentHashMap<>();

    /* 어떤 사용자가 악의적으로 수만자의 긴 텍스트를 검색창에 넣으면 서버 메모리가 감당을 못할 수 있음,
       이러한 상황을 방지하기 위한 방어코드*/
    private static final int MAX_KEYWORD = 20;

    @Transactional(readOnly = true)
    public PopularSearchResponseDto getPopular(String keyword, Pageable pageable) {

        // 검색값 (keyword)가 빈값인지 체크
        if (keyword == null || keyword.isBlank()) {
            // NPE 방지를 위해 빈 객체를 리턴
            return new PopularSearchResponseDto(
                    Page.empty(pageable),
                    Page.empty(pageable),
                    Page.empty(pageable)
            );
        }
        // 양 끝 공백 제거
        String trimKeyword = keyword.trim();

        // 의미 없는 검색 방지 (ex 1글자 검색)
        if (trimKeyword.length() < 2) {
            return new PopularSearchResponseDto(
                    Page.empty(pageable),
                    Page.empty(pageable),
                    Page.empty(pageable)
            );
        }

        // 최대 검색 길이를 초과한 경우 예외 던지기
        if (trimKeyword.length() > MAX_KEYWORD) {
            throw new SearchException(ErrorCode.SEARCH_LENGTH_TOO_LONG);
        }

        // 비어있지 않을 때 검색어 기록
        /* merge -> Map 인터페이스에서 제공하는 메서드
         Map에 값을 넣고 수정할 때 get으로 가져와서 값이 없으면 0을 넣고, 값이 있으면 +1을 해서
         다시 put을 해주는 과정을 거쳐야하는데, merge를 쓰면 이 과정들을 한번에 해줌*/
        ranking.merge(trimKeyword, 1, Integer::sum);
        /* keyword -> 저장하는 데이터 값 (ex: Java)
         1 -> 만약 맵에 해당 키워드가 처음 들어오는 값이라면 기본값을 넣겠다는 의미
         Integer::sum -> 만약 맵에 해당 키워드가 존재한다면, 기존 값과 새 값을 합치는 계산을 해줌
         "keyword가 없으면 1을 넣고, 있으면 기존 점수에 1을 더해라"
         */

        try {
            Page<ProjectPopularSearchResponseDto> projectPage = projectRepository.projectsPopularSearch(trimKeyword, pageable);
            Page<CategoryPopularSearchResponseDto> categoryPage = categoryRepository.categoriesPopularSearch(trimKeyword, pageable);
            Page<SkillPopularSearchResponseDto> skillPage = skillRepository.skillsPopularSearch(trimKeyword, pageable);

            return new PopularSearchResponseDto(projectPage, categoryPage, skillPage);
        } catch (Exception e) {
            log.warn("통합 검색 중 DB 에러 발생 - 키워드: [{}], 메시지: {}", trimKeyword, e.getMessage());
            throw new SearchException(ErrorCode.SEARCH_FAILED);
        }
    }

    // 상위 10개 인기 검색어 순위 조회
    public List<String> getPopularRanking() {
        return ranking.entrySet().stream()
                .sorted(Map.Entry.<String, Integer> comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
    }
    /* 자바의 스트림 API -> ranking 맵에서 인기 검색어 1등부터 10등까지 뽑아냄
     ranking.entrySet().stream() -> entry는 Map에서 key,value 한 쌍을 의미함
     그리고 Set인터페이스를 가리키는 이유는, Set은 순서와 중복을 허용하지 않기 때문임
     Map.Entry -> Map의 Key,Value 쌍을 다루고 있다
     <String, Integer> -> 해당 key는 String 타입이고, value는 Integer 타입이다.
     comparingByValue() -> 점수를 기준으로 잡아라
     reversed() -> 큰 숫자가 앞으로 오게 뒤집어라
     limit -> 10개씩 끊어라
     map(Map.Entry::getKey) -> ::는 메서드 참조 문법,
     ex) [Spring=10, Java-5, Jpa=2] 이런식으로 key,value값들이 있다고 가정하면,
     Map.Entry::getKey -> Key값만 참조해라, 즉 value는 버리고 key값만 가져와라
     변환 후 -> [Spring, Java, Jpa] key값만 남음!*/

}
