package com.example.readys7project.domain.search.keyword;

import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SearchException;
import org.springframework.stereotype.Component;

@Component
public class KeywordValidator {

    // 검색어 길이 체크 및 검증

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 20;

    public boolean isValid(String keyword) {

        if (keyword == null || keyword.isBlank()) return false;

        if (keyword.length() < MIN_LENGTH) return false;

        if (keyword.length() > MIN_LENGTH) {
            throw new SearchException(ErrorCode.SEARCH_LENGTH_TOO_LONG);
        }
        return true;
    }
}
