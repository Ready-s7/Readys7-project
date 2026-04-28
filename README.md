# 🚀 Ready's7 — 개발자 용역 매칭 플랫폼

> **개발자와 의뢰자를 실시간으로 연결하는 용역 매칭 플랫폼**  
> 프로젝트 등록, 제안서 제출, 실시간 채팅, 리뷰까지 — 프리랜서 개발 생태계의 핵심 흐름을 하나의 서비스로 통합했습니다.

<br>

## 📌 목차

1. [서비스 소개](#-서비스-소개)
2. [팀원 소개](#-팀원-소개)
3. [기술 스택](#-기술-스택)
4. [아키텍처](#-아키텍처)
5. [ERD](#-erd)
6. [핵심 기술 구현](#-핵심-기술-구현)
    - [동시성 제어 — Redisson Lock](#1-동시성-제어--redisson-lock)
    - [캐싱 전략 — Redis Cache + 인기 검색어](#2-캐싱-전략--redis-cache--인기-검색어)
    - [검색 API v1 vs v2 — FULLTEXT + ngram Index](#3-검색-api-v1-vs-v2--full-table-scan-vs-fulltext--ngram-index)
    - [인덱스 설계 및 DDL](#4-인덱스-설계-및-ddl)
    - [실시간 채팅 — WebSocket/STOMP + Redis Pub/Sub](#5-실시간-채팅--websocketstomp--redis-pubsub)
    - [CI/CD — GitHub Actions + Docker + AWS](#6-cicd--github-actions--docker--aws)
7. [트러블 슈팅](#-트러블-슈팅)
8. [팀 컨벤션](#-팀-컨벤션)
9. [프로젝트 구조](#-프로젝트-구조)

<br>

---

## 🎯 서비스 소개

### 핵심 문제

| 대상 | 문제 |
|------|------|
| 클라이언트 | 프로젝트에 딱 맞는 기술 스택을 가진 개발자를 찾기 어렵다 |
| 개발자 | 내 스킬에 맞는 프로젝트 기회를 발견하기 어렵다 |
| 기존 플랫폼 | 매칭 효율이 낮고 탐색 비용이 크다 |

### 주요 기능

| 기능 | 설명 | 핵심 기술 |
|------|------|----------|
| 🤝 **매칭 및 제안서 관리** | 개발자·프로젝트 매칭, 프로젝트 등록 및 탐색, 제안서 제출·관리 | Spring Data JPA, QueryDSL |
| 💬 **실시간 채팅** | WebSocket 기반 개발자·의뢰자 실시간 소통, CS 문의 지원 | WebSocket, STOMP, Redis Pub/Sub |
| 🔍 **통합 검색** | 통합 검색 기능, 인기 검색어 제공, Redis 캐싱 기반 성능 향상 | Redis Cache, Redis Sorted Set |
| 🔒 **동시성 제어** | 안정적인 데이터 처리, 분산 락 적용, 낙관적 락 적용 | Redisson (분산 Lock), 낙관적 락 |
| 🚀 **배포 아키텍처** | 무중단 운영 구조, 멀티 서버 환경 구성, 안정적인 서비스 배포 | AWS ALB, EC2 × 2, Docker |

### 서비스 플로우

```
1. 클라이언트   →  프로젝트 등록 (카테고리, 예산, 기간, 스킬 설정)
                            ↓
2. 개발자       →  제안서 제출  ← [Redisson Lock으로 선착순 슬롯 보호]
                            ↓
3. 클라이언트   →  제안서 검토 & 수락
                            ↓
4. 채팅방 생성  →  실시간 소통 [WebSocket / STOMP / Redis Pub/Sub]
                            ↓
5. 프로젝트 완료 → 리뷰 작성 → 개발자 평점 업데이트
```

<br>

---

## 👥 팀원 소개

| 역할 | 이름 | 담당 도메인 | 핵심 기술 구현 |
|------|------|------------|--------------|
| 팀장 | 이석형 | 제안서 도메인, 글로벌 처리, 스킬 도메인 | 테스트코드 작성, 실시간 채팅, CI/CD 및 배포 |
| 팀원 | 최형민 | 리뷰, 포트폴리오 도메인 | 캐싱, 인덱스, 배포 |
| 팀원 | 정호진 | 인증/인가, 클라이언트 도메인 | 캐싱, 인덱스 |
| 팀원 | **류호정** | **프로젝트, 카테고리 도메인** | **동시성 제어** |
| 팀원 | 박수지 | 개발자 도메인, 회의록 | 동시성 제어 |

<br>

---

## 🛠 기술 스택

| 구분 | 기술 스택 | 용도 |
|------|----------|------|
| **Backend** | Java 17, Spring Boot | REST API 서버 개발 |
| **Backend** | Spring Security, JWT | 로그인, 인증/인가 처리 |
| **Database** | MySQL, Spring Data JPA, QueryDSL | 데이터 저장 및 동적 검색 |
| **Cache / Realtime** | Redis, Redisson, WebSocket, STOMP | 캐싱, 분산 락, 실시간 채팅 |
| **Frontend** | React, TypeScript, Vite | 프론트엔드 개발 |
| **Frontend** | Tailwind CSS, MUI, Radix UI | UI 스타일링 및 컴포넌트 |
| **API 통신** | Axios | 프론트-백엔드 통신 |
| **Infra** | Docker, Docker Compose | 로컬/배포 실행 환경 구성 |
| **Deploy** | AWS ALB, EC2, HTTPS | 멀티 앱 서버 배포 및 트래픽 분산 |
| **Deploy** | Route 53, SSL/TLS, 80→443 Redirect | 도메인 연결 및 HTTPS 리다이렉트 |
| **Monitoring** | Actuator, Prometheus, Grafana | 서버 상태 및 메트릭 모니터링 |
| **Test** | JUnit, Spring Boot Test, k6 | 백엔드 테스트 및 부하 테스트 |

<br>

---

## 🏗 아키텍처

### 인프라 구성도 — 무중단 멀티 서버 배포 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                         사용자 (Browser)                         │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                    ① Vercel (프론트 정적 배포)
                               │
                    ② HTTP 80 → HTTPS 443 리다이렉트
                               │
                    ③ Route 53 도메인 연결 + SSL/TLS
                               │
                    ④ Redis (EC2) — 캐시 / 세션 / 분산 Lock
                               │
                    ⑤ AWS ALB (Application Load Balancer)
                          트래픽 분산
                        ┌──────┴──────┐
                        │             │
              ⑥ App 1 (EC2)    App 2 (EC2)
                멀티 앱 서버     멀티 앱 서버
                 [Spring Boot]   [Spring Boot]
                        │             │
                        └──────┬──────┘
                               │
                    ⑦ AWS RDS (MySQL 8.0)
                        공통 데이터 저장소
```

### 무중단 배포 전략 (High Availability)

> ASG(Auto Scaling Group) 없이 **직접 두 개의 앱 서버를 운영**하여 인프라 제어력을 높였습니다.

```
[재배포 시 가동률 100% 유지]

배포 전:  App 1 (Active) ←→ App 2 (Active)   → ALB가 양쪽에 트래픽 분산

배포 중:  App 1 (배포 중, 잠시 다운)          → ALB가 App 2로만 트래픽 라우팅
          App 2 (Active, 서비스 유지 ✅)

배포 후:  App 1 (Active) ←→ App 2 (Active)   → 정상 복귀, 서비스 중단 없음 ✅
```

- 한 서버가 내려가도 다른 서버가 살아있어 **서비스 중단 없이 배포** 가능
- GitHub Actions CI/CD로 빌드 → 테스트 → Docker Push → EC2 순차 배포 자동화

### 애플리케이션 내부 구조

```
┌─────────────────────────────────────────────────────┐
│               Spring Boot Application                │
│                                                     │
│  Controller (ApiResponseDto<T> 공통 응답 포맷)       │
│       ↓                                             │
│  Service ←── LockService (Redisson 분산 Lock)        │
│       ↓                                             │
│  Repository (Spring Data JPA / QueryDSL)            │
└──────────────┬──────────────────────────────────────┘
               │
    ┌──────────┼──────────────────────┐
    │          │                      │
 MySQL      Redis (Lock)        Redis (Cache/Pub/Sub)
 (RDS)      Redisson            Sorted Set / @Cacheable
```

**설계 원칙**
- 계층 분리: `Controller → Service → Repository` 흐름 엄수
- 응답 포맷: 모든 API는 공통 `ApiResponseDto<T>` 객체로 래핑
- Entity 보호: Entity는 절대 Controller 밖으로 노출 금지, `DTO (Record + Builder)`로만 응답
- 예외 처리: 도메인별 `CustomException` + `GlobalExceptionHandler`

<br>

---

## 📊 ERD

```
users (1) ────── (1) clients  ──────────────────── (N) projects ──── (1) categories
users (1) ────── (1) developers ──── (N) portfolios        │
users (1) ────── (1) admins                           (N) proposals
                      │                                    │
               (관리) skills, categories           developer (1)

projects (1) ──── (1) chat_rooms ──── (N) messages
                        │
                   client ↔ developer

developers (1) ──── (N) reviews ──── (1) projects
clients    (1) ──── (N) reviews
```

### 주요 테이블 설명

| 테이블 | 설명 | 비고 |
|--------|------|------|
| `users` | 통합 회원 (CLIENT / DEVELOPER / ADMIN) | Soft Delete (`is_deleted`) |
| `projects` | 클라이언트 등록 프로젝트 | `current_proposal_count`, `max_proposal_count` 슬롯 관리 |
| `proposals` | 개발자의 프로젝트 제안서 | Redisson Lock으로 슬롯 동시성 보호 |
| `categories` | 프로젝트 카테고리 (ADMIN 전용 관리) | Soft Delete |
| `skills` | 기술 스택 마스터 데이터 | ADMIN 전용 관리 |
| `chat_rooms` | 프로젝트 단위 채팅방 | client_id + developer_id |
| `messages` | 채팅 메시지 | 커서 기반 페이징 |
| `reviews` | 프로젝트 완료 후 상호 평가 | 평점 기반 개발자 신뢰도 형성 |
| `portfolios` | 개발자 포트폴리오 | skills JSON 컬럼 |

<br>

---

## ⭐ 핵심 기술 구현

---

### 1. 동시성 제어 — Redisson Lock

#### 📌 문제 상황

인기 프로젝트에 수백 명의 개발자가 동시에 제안서를 제출할 경우, `max_proposal_count`를 초과해 슬롯이 오버부킹되는 **Race Condition** 이 발생합니다.

```
[Lock 없는 상황]
Thread A: currentCount(9) < maxCount(10) → 슬롯 통과 ✅
Thread B: currentCount(9) < maxCount(10) → 슬롯 통과 ✅  ← 동시에 실행!
결과: currentProposalCount = 11  ← 데이터 정합성 파괴 💥
```

#### 🔧 구현 선택 이유 — Redisson vs Lettuce

| 항목 | Lettuce (SETNX) | Redisson (tryLock) |
|------|----------------|-------------------|
| 구현 방식 | 직접 Lua Script + Spin Polling | 내장 Lua Script + Pub/Sub 기반 대기 |
| Busy-Waiting | 있음 (CPU 낭비 발생) | 없음 (Pub/Sub 알림 방식) |
| 자동 해제 (TTL) | 직접 설정 필요 | 내장 지원 |
| 코드 복잡도 | 높음 | 낮음 |
| **선택 결과** | 1차 구현 후 마이그레이션 | **최종 채택** ✅ |

> `LockRedisRepository` (Lettuce/SETNX 방식)는 1차 구현체로 코드에 보존되어 있으며,  
> 최종적으로는 **Redisson** 의 `tryLock` 방식으로 전환했습니다.

#### ⚙️ 구현 구조

```
ProposalService.createProposal()
    └─ LockService.executeWithLock("project:{projectId}")
            └─ redissonClient.getLock(lockKey).tryLock(waitTime=3s, leaseTime=5s)
                    └─ ProposalTransactionalService.createProposalInternal()  ← @Transactional
                            ├─ 슬롯 검증: currentCount >= maxCount → PROPOSAL_SLOT_FULL 예외
                            ├─ 중복 제출 검증 → PROPOSAL_ALREADY_EXISTS 예외
                            ├─ Proposal 저장
                            └─ ProjectService.incrementProposalCount()
```

**왜 Service를 두 개로 분리했나요?**

`LockService`는 트랜잭션 **밖** 에서 Lock을 먼저 획득해야 합니다.  
만약 `@Transactional` 메서드 내부에서 Lock을 잡으면, **트랜잭션 커밋 전에 Lock이 해제** 되어 다른 스레드가 미완성 데이터를 읽는 문제가 생깁니다.

이를 방지하기 위해 `ProposalTransactionalService`를 별도로 분리하여  
**Lock 획득 → 트랜잭션 시작 → DB 작업 → 커밋 → Lock 해제** 순서를 명시적으로 보장했습니다.

#### ✅ 동시성 테스트 결과

```java
// ProposalConcurrencyTest.java
// 100명의 개발자가 슬롯 10개짜리 프로젝트에 동시 제안서 제출
//   CyclicBarrier  : 100명 전원이 모일 때까지 대기 후 동시 출발
//   CountDownLatch : 100명 전원 완료까지 메인 스레드 대기
//   AtomicInteger  : 성공/실패 카운트 (스레드 세이프)
```

| 측정 항목 | Lock 적용 전 | Lock 적용 후 (Redisson) |
|----------|-------------|------------------------|
| 성공한 제안서 수 | 10개 초과 (비결정적) 💥 | **정확히 10개** ✅ |
| DB `currentProposalCount` | 10 초과 (데이터 오염) 💥 | **정확히 10** ✅ |
| 테스트 통과 여부 | FAIL | **PASS** ✅ |

<br>

---

### 2. 캐싱 전략 — Redis Cache + 인기 검색어

#### 📌 Redis Cache (통합 검색 캐싱)

검색 트래픽이 높은 통합 검색(`/v2/search`)에 **Redis Cache** 를 적용하여 반복 조건의 DB 접근을 차단합니다.

```
[CacheConfig.java 핵심 설정]
- TTL           : 5분 (Duration.ofMinutes(5))
- Null 캐싱     : 비활성화 (disableCachingNullValues)
- Key 직렬화    : StringRedisSerializer
- Value 직렬화  : GenericJackson2JsonRedisSerializer (LocalDateTime 처리 포함)
```

데이터 변경 시 캐시 자동 무효화 (`@CacheEvict`) 적용 대상:

| 메서드 | 이유 |
|--------|------|
| `createProject()` | 새 프로젝트 생성 시 검색 결과 변경 |
| `updateProject()` | 프로젝트 정보 수정 시 |
| `deleteProject()` | 프로젝트 삭제 시 |
| `changeProjectStatus()` | 상태 변경 시 (OPEN → CLOSED 등) |
| `createCategory()` | 새 카테고리 추가 시 |
| `updateCategory()` | 카테고리 수정 시 |
| `deleteCategory()` | 카테고리 삭제 시 |

#### 📌 인기 검색어 (Redis Sorted Set)

```
Redis Sorted Set Key : "popular:search"
Member               : 검색 키워드 (예: "Java", "Spring Boot")
Score                : 검색 횟수 (ZINCRBY로 원자적 증가)
조회                 : ZREVRANGE + WITHSCORES → 상위 N개 내림차순
```

| 구현 방식 | 설명 |
|----------|------|
| 실시간 집계 | 검색 시 `ZINCRBY`로 Score 원자적 증가 |
| 랭킹 조회 | `ZREVRANGE`로 상위 N개 내림차순 반환 |
| 동시성 안전 | Redis 단일 커맨드로 원자성 보장 |

#### 📊 캐시 적용 전/후 성능 비교

| 측정 항목 | v1 (캐시 미적용) | v2 (Redis Cache 적용) |
|----------|----------------|----------------------|
| 최초 요청 응답시간 | ~45ms | ~47ms (캐시 저장 비용) |
| 반복 요청 응답시간 | ~45ms | **~3ms** ✅ |
| DB 쿼리 호출 횟수 | 매 요청마다 | **TTL 내 0회** ✅ |
| 데이터 신선도 | 실시간 | TTL 기준 (5분) |

<br>

---

### 3. 검색 API v1 vs v2 — Full Table Scan vs FULLTEXT + ngram Index

검색 기능은 **버전별로 분리 관리** 하여 성능 개선 과정을 명확히 추적할 수 있도록 설계했습니다.

#### v1 — LIKE Full Table Scan (인덱스 없음, 캐시 없음)

```
GET /api/v1/search?keyword=Java
```
- LIKE `%keyword%` 조건으로 `projects`, `developers`, `users` 테이블 Full Table Scan
- 실행 방식: `Table Scan + Filter + Sort`
- 50,000건 데이터 기준: projects 230ms, developers 83ms 소요

#### v2 — FULLTEXT + ngram Index + Redis Cache 적용

```
GET /api/v2/search?keyword=Java
```

**FULLTEXT + ngram을 선택한 이유**

기존 LIKE '%검색어%' 방식은 사용자가 입력한 검색어가 포함된 데이터를 찾을 수는 있지만, 검색 대상 테이블을 전체 스캔해야 하므로 데이터가 많아질수록 성능이 저하된다. 실제 실행계획에서도 LIKE 방식은 Table Scan + Filter + Sort 방식으로 처리되어 실행 시간이 상대적으로 길게 나타났다.
FULLTEXT INDEX를 적용하면 MATCH ... AGAINST 구문을 통해 인덱스 기반 검색이 가능해져 LIKE보다 검색 성능이 개선된다. 그러나 기본 FULLTEXT INDEX는 공백 기준 토큰화에 의존하기 때문에 한글 검색에서는 한계가 있다. 한글은 영어처럼 단어가 공백으로 명확히 분리되지 않는 경우가 많고, 사용자가 검색어 전체가 아닌 일부만 입력하는 경우도 많다.
따라서 우리 조는 단순히 가장 빠른 검색 방식이 아니라, 한글 검색 정확도와 부분 검색 대응력을 높일 수 있는 FULLTEXT + ngram 방식을 선택하였다.
ngram parser는 문자열을 일정 길이의 단위로 나누어 인덱싱하기 때문에 사용자가 검색어 일부만 입력해도 검색 결과를 찾을 수 있다. 실제 결과 자료에서도 경민수, 백두처럼 일부 검색어를 입력했을 때도 결과가 조회되는 것을 확인할 수 있었다.
즉, FULLTEXT + ngram은 일부 상황에서 기본 FULLTEXT INDEX보다 실행 시간이 증가할 수 있지만, 우리 프로젝트에서는 검색 누락을 줄이고 사용자가 원하는 결과를 더 잘 찾게 하는 것이 중요했기 때문에 최종 검색 방식으로 선택하였다.
**Native SQL로 MATCH ... AGAINST 직접 실행**

QueryDSL/JPQL은 `MATCH ... AGAINST` 같은 MySQL FULLTEXT 문법을 자연스럽게 처리하기 어렵기 때문에, `EntityManager`를 활용한 Native SQL로 분리하여 실행합니다.

```sql
-- 프로젝트 FULLTEXT 검색 (Native SQL)
SELECT p.id
FROM projects p
WHERE p.is_deleted = false
  AND (
    MATCH(p.title, p.description) AGAINST (:keyword IN BOOLEAN MODE)
    OR JSON_CONTAINS(p.skills, JSON_QUOTE(:keyword))
  )
ORDER BY p.created_at DESC
LIMIT :limit OFFSET :offset;

-- 개발자 FULLTEXT 검색 (Native SQL)
SELECT d.id
FROM developers d
JOIN users u ON u.id = d.user_id
WHERE d.is_deleted = false
  AND u.is_deleted = false
  AND (
    MATCH(d.title) AGAINST (:keyword IN BOOLEAN MODE)
    OR MATCH(u.name, u.description) AGAINST (:keyword IN BOOLEAN MODE)
    OR JSON_CONTAINS(d.skills, JSON_QUOTE(:keyword))
  )
ORDER BY d.created_at DESC
LIMIT :limit OFFSET :offset;
```

**비동기 병렬 검색 (CompletableFuture)**

4개 도메인(프로젝트, 카테고리, 스킬, 개발자) 검색을 `CompletableFuture`로 병렬 실행하여 응답 속도를 추가로 개선했습니다.

```
검색 요청
    ├─ CompletableFuture: projectRepository.projectsGlobalSearch()   ─┐
    ├─ CompletableFuture: categoryRepository.categoriesGlobalSearch() ─┤ 병렬 실행
    ├─ CompletableFuture: skillRepository.skillsGlobalSearch()        ─┤
    └─ CompletableFuture: developerRepository.developerGlobalSearch() ─┘
                                │
                    CompletableFuture.allOf().join()
                                │
                        GlobalSearchResponseDto 반환
```
**성능 비교**

#### 실행 계획 분석 (EXPLAIN)

| 구분 | 검색 방식 | 대상 테이블 / 컬럼 | 검색어 | 전체 실행 시간 | 주요 처리 방식 | 결과 row 수 |
|------|----------|--------------------|--------|--------------|--------------|------------|
| 1-1 | LIKE (FULLTEXT INDEX 적용 전) | projects.title, projects.description | 백엔드 개발 | 230ms | Table Scan + Filter + Sort | 5 |
| 1-2 | LIKE (FULLTEXT INDEX 적용 전) | developers.title | 백엔드 개발 | 83.1ms | Table Scan + Filter + Sort | 5 |
| 1-3 | LIKE (FULLTEXT INDEX 적용 전) | users.description → developers JOIN | 성능 최적화 | 99.4ms | Table Scan + Nested Loop Join + Sort | 5 |
| 2-1 | FULLTEXT (INDEX 적용 후) | projects.title, projects.description | 백엔드 개발 | 56.4ms | Full-text Index Search + Filter + Sort | 5 |
| 2-2 | FULLTEXT (INDEX 적용 후) | developers.title | 백엔드 개발 | 55.2ms | Full-text Index Search + Filter + Sort | 5 |
| 2-3 | FULLTEXT (INDEX 적용 후) | users.description → developers JOIN | 성능 최적화 | 56.3ms | Full-text Index Search + Nested Loop Join + Sort | 5 |
| **3-1** | **FULLTEXT + ngram (ngram parser 적용 후)** | projects.title, projects.description | 백엔드 개발 | **77.3ms** | Full-text Index Search + Filter + Sort | 5 |
| **3-2** | **FULLTEXT + ngram (ngram parser 적용 후)** | developers.title | 백엔드 개발 | **50.2ms** | Full-text Index Search + Filter + Sort | 5 |
| **3-3** | **FULLTEXT + ngram (ngram parser 적용 후)** | users.name, users.description → developers JOIN | 성능 최적화 | **87.6ms** | Full-text Index Search + Nested Loop Join + Sort | 5 |

#### 실제 통합 검색 결과 비교

| 구분 | 검색 방식 | 응답 시간 | 결과 특징 |
|------|----------|---------|---------|
| v1 | LIKE (Full Scan) | 537ms ~ 601ms | 단순 문자열 포함 검색, 한글 부분 검색 가능 |
| - | FULLTEXT | 907ms | 한글 부분 검색 한계 |
| - | 하이브리드 (LIKE + FULLTEXT) | 539ms ~ 1.13s | 검색어에 따라 결과 불안정 |
| **v2** | **FULLTEXT + ngram** | **233ms ~ 949ms** | **한글 일부 입력 검색에도 정확한 결과 반환** ✅ |

<br>

---

### 4. 인덱스 설계 및 DDL

과제 요구사항에 따라 검색 성능 개선을 위해 적용한 인덱스 DDL을 명시합니다.

#### projects 테이블

```sql
-- 프로젝트 제목 + 설명 통합 검색용 FULLTEXT + ngram 인덱스
ALTER TABLE projects
    ADD FULLTEXT INDEX idx_projects_fulltext_ngram (title, description)
    WITH PARSER ngram;

-- 프로젝트 상태 필터링 인덱스
CREATE INDEX idx_projects_status ON projects (project_status);

-- 카테고리별 프로젝트 조회 인덱스
CREATE INDEX idx_projects_category_id ON projects (category_id);

-- 클라이언트별 프로젝트 조회 인덱스
CREATE INDEX idx_projects_client_id ON projects (client_id);
```

#### developers 테이블

```sql
-- 개발자 타이틀 검색용 FULLTEXT + ngram 인덱스
ALTER TABLE developers
    ADD FULLTEXT INDEX idx_developers_fulltext_ngram (title)
    WITH PARSER ngram;
```

#### users 테이블

```sql
-- 개발자 통합 검색 시 users.name, description JOIN 검색용 FULLTEXT + ngram 인덱스
ALTER TABLE users
    ADD FULLTEXT INDEX idx_users_fulltext_ngram (name, description)
    WITH PARSER ngram;
```

#### skills 테이블

```sql
-- 개발자 통합 검색 시 skills.name 검색용 FULLTEXT + ngram 인덱스
ALTER TABLE skills
    ADD FULLTEXT INDEX idx_skills_name (name)
    WITH PARSER ngram;
```

#### 인덱스 적용 효과 요약

| 테이블 | 인덱스 종류 | 효과 |
|--------|-----------|------|
| `projects` | FULLTEXT ngram (title, description) | LIKE 대비 검색 시간 약 74% 단축 (230ms → 56.4ms) |
| `developers` | FULLTEXT ngram (title) | LIKE 대비 검색 시간 약 40% 단축 (83ms → 50ms) |
| `users` | FULLTEXT ngram (name, description) | 개발자 JOIN 검색 정확도 및 부분 검색 대응력 향상 |

<br>

---

### 5. 실시간 채팅 — WebSocket/STOMP + Redis Pub/Sub

#### 구조

```
클라이언트 A              Spring Boot Server             클라이언트 B
     │                          │                              │
     │  STOMP SEND              │                              │
     │  /send/chat/rooms/{id}   │                              │
     ├─────────────────────────►│                              │
     │                          │  Redis PUBLISH               │
     │                          │  "chat-room:{id}"            │
     │                          ├─────────────────────────────►│  (Pub/Sub)
     │                          │                              │
     │  STOMP SUBSCRIBE         │  SimpMessagingTemplate       │
     │  /receive/chat/rooms/{id}│◄─────────────────────────────┤
     │◄─────────────────────────┤                              │
```

- **JWT ChannelInterceptor** (`StompAuthInterceptor`): WebSocket 연결 시 인증 처리
- **Redis Pub/Sub** (`chat-room:*`, `cs-room:*` 패턴 구독): 서버 확장 시에도 메시지 브로드캐스팅 보장
- **커서 기반 페이징**: 이전 메시지 무한 스크롤 지원 (`MessageCursorResponseDto`)
- **CS 채팅 분리**: 일반 채팅(`ChatRoom/Message`)과 CS 채팅(`CsChatRoom/CsMessage`) 도메인 분리

<br>

---

### 6. CI/CD — GitHub Actions + Docker + AWS

```
코드 Push (main / dev)
       │
       ▼
GitHub Actions CI
   ├─ Gradle Build
   ├─ JUnit / Spring Boot Test 실행
   └─ Docker Image Build & Push
               │
               ▼
    AWS EC2 (순차 무중단 배포)
   ├─ App 1 배포 (App 2가 트래픽 유지)
   └─ App 2 배포 (App 1이 트래픽 유지)
               │
               ▼
    AWS ALB → 두 서버로 트래픽 복귀
```

**운영 환경 설정**

| 항목 | 설정 값                                                     |
|------|----------------------------------------------------------|
| HikariCP 최대 풀 사이즈 | 20                                                        |
| HikariCP 최소 유휴 커넥션 | 10                                                       |
| 모니터링 | Prometheus + Grafana + Actuator (`/actuator/prometheus`) |
| 부하 테스트 | k6                                                       |
| CORS 허용 오리진 | `localhost:5173`, `readys7-project.vercel.app`           |
| 민감 정보 | 환경변수로 분리 (`SPRING_DATASOURCE_URL`, `JWT_SECRET` 등)       |
| HTTPS | Route 53 + SSL/TLS + 80→443 Redirect                     |

<br>

---

## 🔥 트러블 슈팅

### 1. Redisson Lock 도입 — Lettuce(SETNX) Busy-Waiting 문제

**배경**
초기 `LockRedisRepository`를 Lettuce 기반 `SETNX + TTL`로 구현하였습니다.

**발단**
Spin Polling 방식으로 Lock 획득을 시도하다 보니, 대기 중인 스레드들이 지속적으로 Redis에 폴링 요청을 보내 **불필요한 네트워크 트래픽과 CPU 낭비** 가 발생했습니다.

**전개**
100개 스레드 동시 테스트에서 Lock 획득 경쟁이 심화될수록 응답 시간이 선형적으로 증가하는 문제를 발견했습니다.

**해결**
Redisson의 `tryLock`은 내부적으로 **Redis Pub/Sub** 기반의 알림 방식을 사용합니다. Lock 해제 시 대기 중인 스레드에게 알림을 보내는 방식으로, Busy-Waiting 없이 효율적으로 대기합니다.  
`waitTime=3s`, `leaseTime=5s` 설정과 함께 Lock 획득 실패 시 즉시 예외를 반환하는 **Fail Fast** 전략을 채택했습니다.

**결말**
100명 동시 요청 테스트에서 정확히 10개의 제안서만 성공하는 **데이터 정합성 보장** 을 달성했습니다.

---

### 2. @Transactional + Redis Lock 충돌 — Service 레이어 분리

**배경**
`ProposalService.createProposal()`에서 `@Transactional`과 Redisson Lock을 함께 사용하려 했습니다.

**발단**
Lock을 잡은 채로 `@Transactional` 메서드를 실행하면, **트랜잭션 커밋 이전에 Lock이 해제** 되어 커밋 전 데이터를 다른 스레드가 읽는 문제가 발생했습니다.

**해결**
`ProposalService` (Lock 관리, `@Transactional` 없음)와 `ProposalTransactionalService` (실제 DB 작업, `@Transactional` 보유)로 책임을 분리했습니다.  
**Lock 획득 → 트랜잭션 시작 → DB 작업 → 커밋 → Lock 해제** 순서를 명시적으로 보장합니다.

---

### 3. 캐싱 적용 전후 비교 — @CacheEvict 기반 데이터 정합성 유지

**배경**
`@Cacheable`만 적용하여 DB 조회 결과를 키워드 기반으로 Redis에 저장하는 구조로 1차 구현했습니다.

**발단**
신규 개발자 가입 직후 동일 키워드로 검색하면, Redis에 캐싱된 이전 결과가 반환되어 **새로 추가된 개발자가 검색 결과에 나타나지 않는 데이터 불일치** 문제가 발생했습니다.

```
[문제 상황]
신규 개발자 가입 → DB에는 최신 데이터 저장
                    ↓
검색 요청 → Redis 캐시 히트 (주기 업데이트 없음)
                    ↓
검색 결과 누락 💥  ← Redis가 DB 변경을 모름
```

**해결**
데이터 생성·수정 시 캐시를 즉시 무효화하는 `@CacheEvict` 전략을 도입했습니다.  
프로필 생성/수정 → `@CacheEvict` 즉시 실행 → Redis 캐시 삭제 → 다음 검색 시 DB 최신 데이터 기반으로 정확한 결과 반환합니다.

```
[해결 후]
프로필 생성/수정 → @CacheEvict 즉시 실행 → Redis 캐시 삭제
                    ↓
검색 요청 → 캐시 미스 → DB 최신 데이터 조회 → 최신 검색 결과 반영 ✅
```

**결말**
캐시 제거 전엔 검색 직후 불일치가 발생할 수 있었지만, `@CacheEvict` 적용 이후에는 **DB 최신 데이터 기반으로 정확한 결과를 반환**하는 정합성을 확보했습니다.

---

### 4. 백엔드 HTTPS 전환 및 CORS — WebSocket 연결 불안정 해결

**배경**
서비스를 HTTP 환경에서 운영 중이었으며 프론트엔드(Vercel)와 백엔드(EC2) 간 통신에 문제가 발생했습니다.

**발단**
HTTP 환경에서는 보안 취약점이 존재하고, 특히 **WebSocket 연결이 불안정**하여 실시간 채팅 기능이 정상적으로 동작하지 않았습니다.

**전개 — 3단계 기술적 해결 과정**

1. **CORS 정책 및 환경 변수 동기화**
    - 프론트 허용 도메인과 API/WebSocket 환경 변수를 맞춰 CORS 오류와 API Base URL을 최소화
    - `.env` 기반 환경 변수 분리 관리

2. **HTTPS 인프라 체계 구축**
    - AWS ALB + SSL/TLS 인증서를 통해 HTTPS 통신 기반 마련
    - HTTP 80 → HTTPS 443 리다이렉트 적용

3. **프록시 및 포워드 인식 보강 + WebSocket 직접 연결 안정성 확보**
    - Spring Boot가 `X-Forwarded-Proto` 헤더를 신뢰하도록 설정하여 내부 프로토콜 문제 해결
    - Vercel Proxy와 별도로 도메인 직접 연결로 **WSS(WebSocket Secure) 통신 안정성 확보**

**결말**
HTTPS 전환 이후 WebSocket 연결이 안정화되었으며, CORS 오류 없이 프론트엔드와 백엔드 간 정상 통신을 달성했습니다.

---

### 5. Redis Cache 직렬화 — LocalDateTime 처리 오류

**배경**
`GenericJackson2JsonRedisSerializer`를 사용했으나 `LocalDateTime` 필드가 역직렬화 과정에서 타입 불일치 오류를 발생시켰습니다.

**해결**
`RedisConfig`에서 `JavaTimeModule` 등록, `WRITE_DATES_AS_TIMESTAMPS` 비활성화, `DefaultTyping` (타입 정보 포함) 설정을 담은 전용 `ObjectMapper`를 별도 생성하여 `GenericJackson2JsonRedisSerializer`에 주입했습니다.

<br>

---

## 📐 팀 컨벤션

### 브랜치 전략

```
main    ← 배포용 (최종 merge)
  └── dev  ← 통합 개발 브랜치
        └── feat/(작업명칭)/#(이슈번호)  ← 기능 단위 브랜치
```

### 커밋 컨벤션

| 타입 | 설명 |
|------|------|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `refactor` | 리팩토링 |
| `perf` | 성능 개선 |
| `chore` | 자잘한 수정 / 빌드 |
| `ci` | CI 설정 수정 |

> 커밋 형식: `feat(#이슈번호) : 작업내용`

### 코딩 컨벤션

| 항목 | 규칙 |
|------|------|
| 변수명 | 카멜케이스 (`createProject`, `getProjects`) |
| 클래스명 | 파스칼케이스, DTO는 `Dto` 접미사 필수 (`ProjectCreateRequestDto`) |
| 패키지명 | 소문자 (`domain/project/dto/request/`) |
| DTO 구조 | `Record + Builder` 패턴 고정 |
| 쿼리 | 단순 조회는 JPA, 복잡한 조건은 QueryDSL |
| PR 규칙 | PR 생성 시 All Stop → Code Review → Sync → 작업 재개 |

<br>

---

## 🗂 프로젝트 구조

```
backend/src/main/java/com/example/readys7project/
├── config/
│   ├── CacheConfig.java             # Redis Cache 설정 (TTL 5분)
│   ├── RedisConfig.java             # Redisson, RedisTemplate, Pub/Sub 설정 (박수지)
│   ├── SecurityConfig.java          # Spring Security 설정
│   └── WebSocketConfig.java         # STOMP 웹소켓 설정
│
├── domain/
│   ├── project/                     # 프로젝트 도메인 (류호정)
│   │   ├── entity/Project.java      # skillsText 섀도우 컬럼 + @PrePersist 동기화
│   │   └── service/ProjectService.java  # @CacheEvict 적용
│   ├── category/                    # 카테고리 도메인 (류호정)
│   ├── proposal/                    # 제안서 도메인 (이석형)
│   │   ├── service/ProposalService.java             # Lock 관리 (류호정, 박수지)
│   │   └── service/ProposalTransactionalService.java # @Transactional DB 작업
│   ├── search/                      # 통합 검색 + 인기 검색어 (정호진)
│   │   └── controller/SearchController.java  # v1, v2 버전 분리
│   ├── chat/                        # 실시간 채팅 (이석형)
│   │   ├── chatroom/
│   │   ├── message/
│   │   └── cs/                      # CS 채팅 (별도 분리)
│   ├── review/                      # 리뷰 (최형민)
│   ├── portfolio/                   # 포트폴리오 (최형민)
│   └── user/                        
│       ├── developer/               # 개발자 (박수지)
│       ├── client/                  # 클라이언트 (정호진)
│       ├── admin/                   # 관리자 (정호진)
│       └── auth/                    # 인증 절차 (이석형)
│
└── global/
    ├── lock/
    │   ├── repository/LockRedisRepository.java   # Lettuce SETNX (1차 구현 / 보존)
    │   └── service/LockService.java              # Redisson tryLock (최종 채택)
    ├── exception/
    │   ├── common/GlobalExceptionHandler.java
    │   └── domain/                               # 도메인별 Custom Exception
    └── security/                                 # JWT, CustomUserDetails, StompAuthInterceptor
```