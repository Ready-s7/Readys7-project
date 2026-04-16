package com.example.readys7project.domain.search.Repository;

import com.example.readys7project.domain.search.entity.SearchRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchRankingRepository extends JpaRepository<SearchRanking, Long> {
    Optional<SearchRanking> findByKeywordAndIsDeletedFalse(String keyword);

    List<SearchRanking> findTop10ByIsDeletedFalseOrderBySearchCountDesc();
}
