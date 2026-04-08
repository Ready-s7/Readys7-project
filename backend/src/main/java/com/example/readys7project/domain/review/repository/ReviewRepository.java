package com.example.readys7project.domain.review.repository;

import com.example.readys7project.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByDeveloperId(Long developerId);
    List<Review> findByClientId(Long clientId);
    List<Review> findByProjectId(Long projectId);
}
