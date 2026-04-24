package com.example.readys7project.domain.search.util;

import org.springframework.data.domain.Pageable;

public class SearchCacheKeyGenerator {

    public static String generate(String keyword, Pageable pageable) {

        return "v2:" +
                keyword + ":" +
                pageable.getPageNumber() + ":" +
                pageable.getPageSize();
    }
    // pageable.getPageNumber() + ":" + -> 이부분에 +1을 해줄지 말지 논의
    // 현재 결과 키 : globalSearch::v2:스프링:0:5
    // 단, 캐시키는 사람이 읽는게 아니라 시스템이 매칭하는 용도라서 0이어도 기능상 문제 없음,
    // 가독성 차원에서 +1을 할지 말지 결정하면 됨
}
