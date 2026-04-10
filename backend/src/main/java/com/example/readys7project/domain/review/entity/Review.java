package com.example.readys7project.domain.review.entity;

import com.example.readys7project.domain.developer.entity.Developer;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.domain.user.auth.entity.User;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Builder
    public Review(Developer developer, User client, Project project, Integer rating, String comment) {
        this.developer = developer;
        this.client = client;
        this.project = project;
        this.rating = rating;
        this.comment = comment;
    }
}
