# 프리랜서 개발자 매칭 플랫폼 백엔드

Spring Boot 기반의 프리랜서 개발자 매칭 플랫폼 REST API 서버입니다.

## 기술 스택

- **Java 17**
- **Spring Boot 3.2.4**
- **Spring Security** (JWT 인증)
- **Spring Data JPA**
- **MySQL** (개발환경)
- **MySQL** (운영환경)
- **Gradle**
- **Lombok**

## 주요 기능

### 인증 & 사용자 관리
- 회원가입 (클라이언트/개발자)
- 로그인 (JWT 토큰 발급)
- 사용자 프로필 관리

### 프로젝트 관리
- 프로젝트 생성, 조회, 수정, 삭제
- 프로젝트 검색 및 필터링 (카테고리, 상태, 기술스택)
- 프로젝트 상태 관리 (OPEN, IN_PROGRESS, COMPLETED)

### 개발자 관리
- 개발자 프로필 조회
- 개발자 검색 (기술스택, 지역, 평점)
- 개발자 프로필 업데이트
- 포트폴리오 관리

### 제안서 관리
- 프로젝트에 제안서 제출
- 제안서 조회 (프로젝트별, 개발자별)
- 제안서 상태 변경 (PENDING, ACCEPTED, REJECTED)

### 리뷰 시스템
- 프로젝트 완료 후 리뷰 작성
- 개발자별 리뷰 조회
- 개발자 평점 자동 계산

## 프로젝트 구조

```
backend/
├── src/main/java/com/freelancer/
│   ├── FreelancerPlatformApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── WebConfig.java
│   ├── controller/
│   │   ├── AuthController.java
│   │   ├── ProjectController.java
│   │   ├── DeveloperController.java
│   │   ├── ProposalController.java
│   │   └── ReviewController.java
│   ├── dto/
│   │   ├── AuthResponse.java
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── ProjectDto.java
│   │   ├── ProjectRequest.java
│   │   ├── DeveloperDto.java
│   │   ├── DeveloperProfileRequest.java
│   │   ├── ProposalDto.java
│   │   ├── ProposalRequest.java
│   │   ├── ReviewDto.java
│   │   └── ReviewRequest.java
│   ├── entity/
│   │   ├── User.java
│   │   ├── Developer.java
│   │   ├── Project.java
│   │   ├── Proposal.java
│   │   └── Review.java
│   ├── exception/
│   │   ├── ErrorResponse.java
│   │   └── GlobalExceptionHandler.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── DeveloperRepository.java
│   │   ├── ProjectRepository.java
│   │   ├── ProposalRepository.java
│   │   └── ReviewRepository.java
│   ├── security/
│   │   ├── CustomUserDetailsService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtTokenProvider.java
│   └── service/
│       ├── AuthService.java
│       ├── ProjectService.java
│       ├── DeveloperService.java
│       ├── ProposalService.java
│       └── ReviewService.java
└── src/main/resources/
    └── application.yml
```

## 설치 및 실행

### 1. 프로젝트 클론

```bash
cd backend
```

### 2. 의존성 설치 및 빌드

```bash
./mvnw clean install
```

### 3. 애플리케이션 실행

```bash
./mvnw spring-boot:run
```

서버가 `http://localhost:8080/api`에서 실행됩니다.

## 환경 설정

### application.yml 주요 설정

```yaml
# 데이터베이스 설정
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/readys7  # H2 (개발)
    # url: jdbc:postgresql://localhost:5432/freelancer_db  # PostgreSQL (운영)

# JWT 설정
jwt:
  secret: your-secret-key-here  # 운영환경에서 반드시 변경
  expiration: 86400000  # 24시간

# CORS 설정
cors:
  allowed-origins: http://localhost:3000,http://localhost:5173
```

### MySQL 사용 (운영환경)

1. `application.yml`에서 MySQL 설정
2. MySQL 데이터베이스 생성:

```sql
CREATE DATABASE readys7;
```

## API 엔드포인트

### 인증 API

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | 회원가입 | No |
| POST | `/api/auth/login` | 로그인 | No |

### 프로젝트 API

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/projects` | 모든 프로젝트 조회 | No |
| GET | `/api/projects/{id}` | 프로젝트 상세 조회 | No |
| GET | `/api/projects/search` | 프로젝트 검색 | No |
| POST | `/api/projects` | 프로젝트 생성 | Yes (CLIENT) |
| PUT | `/api/projects/{id}` | 프로젝트 수정 | Yes (CLIENT) |
| DELETE | `/api/projects/{id}` | 프로젝트 삭제 | Yes (CLIENT) |

### 개발자 API

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/developers` | 모든 개발자 조회 | No |
| GET | `/api/developers/{id}` | 개발자 상세 조회 | No |
| GET | `/api/developers/search` | 개발자 검색 | No |
| PUT | `/api/developers/profile` | 프로필 업데이트 | Yes (DEVELOPER) |

### 제안서 API

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/proposals` | 제안서 제출 | Yes (DEVELOPER) |
| GET | `/api/proposals/project/{projectId}` | 프로젝트별 제안서 조회 | Yes |
| GET | `/api/proposals/my-proposals` | 내 제안서 조회 | Yes (DEVELOPER) |
| PATCH | `/api/proposals/{id}/status` | 제안서 상태 변경 | Yes (CLIENT) |

### 리뷰 API

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/reviews` | 리뷰 작성 | Yes (CLIENT) |
| GET | `/api/reviews/developer/{developerId}` | 개발자 리뷰 조회 | No |
| GET | `/api/reviews/project/{projectId}` | 프로젝트 리뷰 조회 | No |

## API 사용 예시

### 회원가입

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "client@example.com",
    "password": "password123",
    "name": "홍길동",
    "role": "CLIENT",
    "location": "서울"
  }'
```

### 로그인

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "client@example.com",
    "password": "password123"
  }'
```

### 프로젝트 생성 (인증 필요)

```bash
curl -X POST http://localhost:8080/api/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -d '{
    "title": "쇼핑몰 웹사이트 개발",
    "description": "React와 Node.js를 사용한 쇼핑몰 개발",
    "category": "web",
    "budget": "300-500만원",
    "duration": "2개월",
    "skills": ["React", "Node.js", "MongoDB"]
  }'
```

### 개발자 검색

```bash
curl "http://localhost:8080/api/developers/search?skill=React&location=서울&minRating=4.5"
```

## 보안 설정

- JWT 기반 인증
- BCrypt 비밀번호 암호화
- CORS 설정
- Stateless 세션 관리

## 데이터베이스 스키마

### users
- id (PK)
- email (Unique)
- password
- name
- role (CLIENT, DEVELOPER)
- phone_number
- location
- avatar_url
- description
- active
- created_at
- updated_at

### developers
- id (PK)
- user_id (FK)
- title
- rating
- review_count
- completed_projects
- hourly_rate
- response_time
- available_for_work

### projects
- id (PK)
- client_id (FK)
- title
- description
- category
- budget
- duration
- status
- proposal_count
- posted_date
- updated_at

### proposals
- id (PK)
- project_id (FK)
- developer_id (FK)
- cover_letter
- proposed_budget
- proposed_duration
- status
- submitted_at

### reviews
- id (PK)
- developer_id (FK)
- client_id (FK)
- project_id (FK)
- rating (1-5)
- comment
- created_at

## 프론트엔드 연동

프론트엔드에서 사용하기 위한 설정:

1. **Base URL**: `http://localhost:8080/api`
2. **Authorization Header**: `Bearer {JWT_TOKEN}`
3. **CORS**: `localhost:3000`, `localhost:5173` 허용됨

## 개발 도구

- **IDE**: IntelliJ IDEA, Eclipse, VS Code
- **API 테스트**: Postman, Insomnia, cURL
- **데이터베이스 관리**: MySql, DBeaver, pgAdmin

## 라이센스

MIT License
