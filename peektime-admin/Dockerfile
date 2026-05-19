# ============================================
# Build stage
# ============================================
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# 루트 Gradle 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .

# Docker 빌드용 settings.gradle (peektime-admin만 포함)
COPY peektime-admin/settings.gradle.docker settings.gradle

# peektime-admin 모듈 복사
COPY peektime-admin peektime-admin

RUN chmod +x ./gradlew

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew :peektime-admin:dependencies --no-daemon || true

# 빌드
RUN ./gradlew :peektime-admin:bootJar --no-daemon

# ============================================
# Runtime stage
# ============================================
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/peektime-admin/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]