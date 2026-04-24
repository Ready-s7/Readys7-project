package com.example.readys7project.domain.search.repository;

import com.example.readys7project.domain.search.entity.SearchRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchRankingRepository extends JpaRepository<SearchRanking, Long> {
    Optional<SearchRanking> findByKeywordAndIsDeletedFalse(String keyword);

    List<SearchRanking> findTop10ByIsDeletedFalseOrderBySearchCountDesc();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE SearchRanking s SET s.searchCount = s.searchCount + 1 WHERE s.keyword = :keyword AND s.isDeleted = false")
    void incrementSearchCount(@Param("keyword") String keyword);
}
