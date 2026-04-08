package com.example.readys7project.domain.developer.entity;

import com.example.readys7project.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "developers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Developer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String title; // 예: "풀스택 개발자"

    @Column(nullable = false)
    private Double rating = 0.0;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount = 0;

    @Column(name = "completed_projects", nullable = false)
    private Integer completedProjects = 0;

    @ElementCollection
    @CollectionTable(name = "developer_skills", joinColumns = @JoinColumn(name = "developer_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @Column(name = "hourly_rate")
    private String hourlyRate; // 예: "5-7만원"

    @Column(name = "response_time")
    private String responseTime; // 예: "1시간 이내"

    @ElementCollection
    @CollectionTable(name = "developer_portfolio", joinColumns = @JoinColumn(name = "developer_id"))
    @Column(name = "portfolio_url")
    private List<String> portfolio = new ArrayList<>();

    @Column(name = "available_for_work", nullable = false)
    private Boolean availableForWork = true;
}
