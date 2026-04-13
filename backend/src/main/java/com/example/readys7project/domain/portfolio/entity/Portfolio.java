package com.example.readys7project.domain.portfolio.entity;


import com.example.readys7project.domain.user.developer.entity.Developer;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@SoftDelete
@Entity
@Table(name = "portfolios")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Portfolio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = false)
    private Developer developer;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "image_url", length = 2083)
    private String imageUrl;

    @Column(name = "project_url", length = 2083)
    private String projectUrl;

    @Builder
    public Portfolio(Developer developer, String title, String description, String imageUrl, String projectUrl) {
        this.developer = developer;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.projectUrl = projectUrl;
    }

    public void update(String title, String description, String imageUrl, String projectUrl) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.projectUrl = projectUrl;
    }
}
