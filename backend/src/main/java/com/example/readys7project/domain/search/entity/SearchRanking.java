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

    @Column(unique = true, nullable = false)
    private String keyword;

    @Column(nullable = false)
    private Integer searchCount;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    public SearchRanking(String keyword) {
        this.keyword = keyword;
        this.searchCount = 1; // 첫 검색은 1부터 시작
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
