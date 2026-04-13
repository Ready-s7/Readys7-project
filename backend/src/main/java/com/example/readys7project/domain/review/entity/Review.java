package com.example.readys7project.domain.review.entity;

import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.domain.project.entity.Project;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE reviews SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 개발자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    // 클라이언트
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;


    // 프로젝트
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // 리뷰 평점
    @Column(nullable = false)
    private Integer rating; // 1-5

    // 리뷰 코멘트
    @Column(columnDefinition = "TEXT", nullable = false)
    private String comment;

    private boolean isDeleted = false;

    @Builder
    public Review(Developer developer,Client client ,Project project, Integer rating, String comment) {
        this.developer = developer;
        this.client=client;
        this.project = project;
        this.rating = rating;
        this.comment = comment;
    }

    // 리뷰 수정 메서드
    public void update(Integer rating, String comment){
        this.rating = rating;
        this.comment = comment;
    }
}
