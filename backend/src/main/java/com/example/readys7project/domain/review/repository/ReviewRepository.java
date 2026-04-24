package com.example.readys7project.domain.review.repository;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.review.entity.Review;
import com.example.readys7project.domain.review.enums.ReviewRole;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.developer.entity.Developer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewQueryRepository {


    List<Review> findByDeveloperId(Long developerId);
    List<Review> findByClientId(Long clientId);

    @Query("SELECT AVG(r.rating), COUNT(r) FROM Review r WHERE r.developer.id = :id AND r.writerRole = 'CLIENT'")
    List<Object[]> getDeveloperRatingSummary(@Param("id") Long id);

    @Query("SELECT AVG(r.rating), COUNT(r) FROM Review r WHERE r.client.id = :id AND r.writerRole = 'DEVELOPER'")
    List<Object[]> getClientRatingSummary(@Param("id") Long id);

    // 리뷰 중복 체크.
    boolean existsByProjectAndClientAndDeveloperAndWriterRole(
            Project project,
            Client client,
            Developer developer,
            ReviewRole writerRole
    );
}
