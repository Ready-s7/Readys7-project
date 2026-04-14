package com.example.readys7project.domain.category.entity;

import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE categories SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "icon")
    private String icon;

    @Column(name = "description")
    private String description;

    @Column(nullable = false, name = "display_order")
    private Integer displayOrder; // DB 정렬을 위한 컬럼

    private boolean isDeleted = false;

    @Builder
    public Category(Admin admin, String name, String icon, String description, Integer displayOrder) {
        this.admin = admin;
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public void updateCategory(
            String name,
            String icon,
            String description,
            Integer displayOrder
    ) {
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
