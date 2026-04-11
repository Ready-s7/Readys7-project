# ==============================
# 1단계: 빌드 (JDK 필요)
# ==============================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Gradle Wrapper 및 의존성 파일 먼저 복사 (레이어 캐싱 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 먼저 다운로드 (코드 변경 시 이 레이어는 재사용)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스코드 복사 후 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

# ==============================
# 2단계: 실행 (JRE만 있으면 됨 → 이미지 경량화)
# ==============================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]