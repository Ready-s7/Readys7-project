package com.example.readys7project.domain.search.keyword;

import org.springframework.stereotype.Component;

@Component
public class KeywordNormalizer {

    // 내부적으로 저장할 때 사용
    public String normalize(String keyword) {
        return keyword.trim().toLowerCase();
    }

    // 화면에 보여줄 때 사용
    public String display(String keyword) {
        String trimmedKeyword = keyword.trim();

        if (trimmedKeyword.isEmpty()) return trimmedKeyword;

        // 첫 글자만 대문자 (ex Java, React)
        return trimmedKeyword.substring(0, 1).toLowerCase()
                + trimmedKeyword.substring(1).toLowerCase();
    }
}
