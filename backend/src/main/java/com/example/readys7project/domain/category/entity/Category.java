package com.example.readys7project.domain.category.entity;

import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String icon;

    private String description;

    @Column(nullable = false)
    private Integer displayOrder; // DB 정렬을 위한 컬럼

    @Builder
    public Category(String name, String icon, String description, Integer displayOrder) {
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
