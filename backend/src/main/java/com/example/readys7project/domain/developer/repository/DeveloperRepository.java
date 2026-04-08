package com.example.readys7project.domain.developer.repository;

import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Long> {
    Optional<Developer> findByUser(User user);
    Optional<Developer> findByUserId(Long userId);

    @Query("SELECT d FROM Developer d WHERE " +
           "(:skill IS NULL OR :skill MEMBER OF d.skills) AND " +
           "(:location IS NULL OR d.user.location = :location) AND " +
           "(:minRating IS NULL OR d.rating >= :minRating) AND " +
           "d.availableForWork = true")
    List<Developer> searchDevelopers(
        @Param("skill") String skill,
        @Param("location") String location,
        @Param("minRating") Double minRating
    );
}
