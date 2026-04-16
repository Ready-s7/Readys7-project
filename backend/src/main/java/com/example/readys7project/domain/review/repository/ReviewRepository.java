package com.example.readys7project.domain.review.repository;

import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.review.entity.Review;
import com.example.readys7project.domain.review.enums.ReviewRole;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.developer.entity.Developer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewQueryRepository {


    List<Review> findByDeveloperId(Long developerId);
    List<Review> findByClientId(Long clientId);

    // 리뷰 중북 체크.
    boolean existsByProjectAndClientAndDeveloperAndWriterRole(
            Project project,
            Client client,
            Developer developer,
            ReviewRole writerRole
    );
}
