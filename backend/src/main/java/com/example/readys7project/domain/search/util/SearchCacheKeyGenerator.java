package com.example.readys7project.domain.search.util;

import org.springframework.data.domain.Pageable;

public class SearchCacheKeyGenerator {

    public static String generate(String keyword, Pageable pageable) {

        return keyword + ":" +
                pageable.getPageNumber() + ":" +
                pageable.getPageSize();
    }
}
