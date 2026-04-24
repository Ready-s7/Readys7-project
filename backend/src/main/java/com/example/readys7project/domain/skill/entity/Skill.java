package com.example.readys7project.domain.skill.entity;

import com.example.readys7project.domain.skill.dto.request.UpdateSkillRequestDto;
import com.example.readys7project.domain.skill.enums.SkillCategory;
import com.example.readys7project.domain.user.admin.entity.Admin;
import com.example.readys7project.global.entity.BaseEntity;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.SkillException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "skills")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE skills SET is_deleted = true WHERE id = ?") // 삭제 시 실행될 SQL 커스텀
@SQLRestriction("is_deleted = false")
public class Skill extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private SkillCategory skillCategory;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Builder
    public Skill(Admin admin, String name, SkillCategory skillCategory) {
        this.admin = admin;
        this.name = name;
        this.skillCategory = skillCategory;
    }

    public void updateSkill(UpdateSkillRequestDto request) {
        if (request.name() != null && !request.name().isBlank()) {
            this.name = request.name();
        }
        if (request.category() != null) {
            this.skillCategory = request.category();
        }
    }
}
