package com.example.readys7project.domain.search.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "search_rankings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", unique = true, nullable = false)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private Integer searchCount;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    public SearchRanking(String keyword) {
        this.keyword = keyword;
        this.searchCount = 0; // increaseSearchCount() 호출시 1 증가 완전 처음 생성일때는 0
    }

    // 카운트 증가
    public void increaseSearchCount() {
        // 최대치에 도달하면 더 이상 올리지 않음
        if (this.searchCount >= Integer.MAX_VALUE) {
            return;
        }
        this.searchCount++;
    }

    // 카운트 감소
    public void decreaseSearchCount() {
        if (this.searchCount <= 0) {
            this.searchCount = 0;
            return;
        }
        this.searchCount--;
    }
}
