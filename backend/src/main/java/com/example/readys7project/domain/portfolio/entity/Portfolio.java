package com.example.readys7project.domain.portfolio.entity;


import com.example.readys7project.domain.portfolio.dto.request.PortfolioRequestDto;
import com.example.readys7project.domain.portfolio.dto.request.PortfolioUpdateRequestDto;
import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.global.converter.StringListConverter;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;


@Entity
@Table(name = "portfolios")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE portfolios SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
@Getter
public class Portfolio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "image_url", length = 2083)
    private String imageUrl;

    @Column(name = "project_url", length = 2083)
    private String projectUrl;

    @Convert(converter = StringListConverter.class)
    @Column(name = "skills", columnDefinition = "json")
    private List<String> skills;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;


    @Builder
    public Portfolio(Developer developer, String title, String description, String imageUrl, String projectUrl, List<String> skills) {
        this.developer = developer;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.projectUrl = projectUrl;
        this.skills = skills;
    }

    public void portfolioUpdate(PortfolioUpdateRequestDto request) {
        if (request.title() != null && !request.title().isBlank()) {
            this.title = request.title();
        }

        if (request.description() != null && !request.description().isBlank()) {
            this.description = request.description();
        }

        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            this.imageUrl = request.imageUrl();
        }

        if (request.projectUrl() != null && !request.projectUrl().isBlank()) {
            this.projectUrl = request.projectUrl();
        }

        if (request.skills() != null) {
            this.skills = request.skills();
        }
    }
}
