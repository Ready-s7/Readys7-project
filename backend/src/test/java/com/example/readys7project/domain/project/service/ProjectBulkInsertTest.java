package com.example.readys7project.domain.project.service;

import com.example.readys7project.domain.category.entity.Category;
import com.example.readys7project.domain.category.repository.CategoryRepository;
import com.example.readys7project.domain.user.client.entity.Client;
import com.example.readys7project.domain.user.client.repository.ClientRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Tag("bulk")
@SpringBootTest
@ActiveProfiles("test")
public class ProjectBulkInsertTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void insert_50000_projects() {

        List<Long> clientIds = clientRepository.findAll()
                .stream().map(Client::getId).toList();

        List<Long> categoryIds = categoryRepository.findAll()
                .stream().map(Category::getId).toList();

        String[] titles = {
                "Java 백엔드 API 개발", "Spring Boot 서버 구축",
                "React 프론트엔드 개발", "Python AI 모델 개발",
                "Docker 인프라 구성", "MySQL 데이터베이스 최적화",
                "Node.js API 서버 개발", "TypeScript 웹 애플리케이션",
                "Kotlin 모바일 앱 개발", "AWS 클라우드 인프라"
        };

        String[] descriptions = {
                "Java와 Spring Boot를 활용한 REST API 개발 프로젝트입니다.",
                "React와 TypeScript 기반 웹 애플리케이션 개발입니다.",
                "Python과 TensorFlow를 활용한 AI 모델 개발입니다.",
                "Docker와 Kubernetes 기반 인프라 구성 프로젝트입니다.",
                "MySQL 성능 최적화 및 쿼리 튜닝 프로젝트입니다."
        };

        String[] skillSets = {
                "[\"Java\", \"Spring Boot\", \"MySQL\"]",
                "[\"React\", \"TypeScript\", \"Node.js\"]",
                "[\"Python\", \"TensorFlow\", \"Docker\"]",
                "[\"Kotlin\", \"AWS\", \"Redis\"]",
                "[\"Vue.js\", \"JavaScript\", \"MySQL\"]"
        };

        int totalCount = 50000;
        int batchSize = 1000;
        Random random = new Random();
        List<Object[]> batch = new ArrayList<>();

        for (int i = 1; i <=totalCount; i++) {
            Long clientId = clientIds.get(random.nextInt(clientIds.size()));
            Long categoryId = categoryIds.get(random.nextInt(categoryIds.size()));
            String title = titles[random.nextInt(titles.length)] + " " + i;
            String description = descriptions[random.nextInt(descriptions.length)];
            String skills = skillSets[random.nextInt(skillSets.length)];
            long minBudget = (random.nextInt(90) + 10) * 100000L;
            long maxBudget = minBudget + (random.nextInt(50) + 10) * 100000L;
            int duration = random.nextInt(90) * 30;

            batch.add(new Object[]{
                    clientId, categoryId, title, description,
                    skills, minBudget, maxBudget, duration,
                    "OPEN", 0, 10, false
            });

            if (batch.size() == batchSize) {
                flushBatch(batch);
                batch.clear();
                System.out.println("[BulkInsert] " + i + "건 insert 완료");
            }
        }
        if (!batch.isEmpty()) {
            flushBatch(batch);
        }
        System.out.println("[BulkInsert] 총 " + totalCount + "건 insert 완료!");
    }

    private void flushBatch(List<Object[]> batch) {
        String sql = """
                INSERT INTO projects
                (client_id, category_id, title, description, skills,
                min_budget, max_budget, duration, project_status,
                current_proposal_count, max_proposal_count, is_deleted)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.batchUpdate(sql, batch);
    }
}
