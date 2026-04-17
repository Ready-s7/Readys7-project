package com.example.readys7project.domain.search.util;

import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SearchException;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ValidateSearchKeyword {

    /* 어떤 사용자가 악의적으로 수만자의 긴 텍스트를 검색창에 넣으면 서버 메모리가 감당을 못할 수 있음,
       이러한 상황을 방지하기 위한 방어코드*/
    private static final int MAX_KEYWORD = 20;

    // 특수 문자 차단 패턴 추가
    private static final Pattern INVALID_PATTERN = Pattern.compile("[<>\"'%;()&+\\-=]");

    // 키워드 검증 공용 메서드
    public String validateSearchKeyword(String keyword) {

        // 키워드가 null이거나 비어있으면 null 리턴
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        // 양끝 공백 제거
        String trimKeyword = keyword.trim().toLowerCase();

        // 최소글자 2글자로 제한 (의미없는 검색 방지)
        if (trimKeyword.length() < 2) {
            return null;
        }

        // 최대 글자수 제한 (악의적 검색 방지)
        if (trimKeyword.length() > MAX_KEYWORD) {
            throw new SearchException(ErrorCode.SEARCH_LENGTH_TOO_LONG);
        }

        // 특수 문자 차단
        if (INVALID_PATTERN.matcher(trimKeyword).find()) {
            throw new SearchException(ErrorCode.SEARCH_INVALID_CHARACTER);
        }

        return trimKeyword;
    }
}
