package com.example.readys7project.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();

        // 통합 검색용 1분으로 설정
        CaffeineCache totalSearchCache = new CaffeineCache("totalSearch",
                Caffeine.newBuilder()
                        // 데이터가 캐시에 저장된 후 1분 후 삭제되게 설정
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        // 최대 100개가 들어갈 수 있게 설정
                        .maximumSize(100)
                        .build());

        // List.of()를 사용해서 컬렉션으로 만들어주고 반환
        simpleCacheManager.setCaches(Collections.singletonList(totalSearchCache));
        return simpleCacheManager;
    }
}
// Collections.singletonList -> 딱 하나만 들어있는 리스트를 만들 때 가장 가볍고 효율적
// 나중에 다른 캐시가 추가되면 Arrays.asList(cache1, cache2 ...) 이런식으로 늘리면 됨
