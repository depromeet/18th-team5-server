# ============================================
# Build stage (루트에서 빌드)
# ============================================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# 루트 Gradle 파일 복사
COPY gradlew .
COPY gradle gradle

# Docker 빌드용 build.gradle, settings.gradle 사용 (peektime-api만 포함)
COPY peektime-api/build.gradle.docker build.gradle
COPY peektime-api/settings.gradle.docker settings.gradle

# peektime-api 모듈 복사
COPY peektime-api peektime-api

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew :peektime-api:dependencies --no-daemon || true

# 빌드
RUN ./gradlew :peektime-api:bootJar --no-daemon

# ============================================
# Runtime stage
# ============================================
FROM eclipse-temurin:25-jre

WORKDIR /app

# curl 설치 (헬스체크용)
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 복사
COPY --from=builder /app/peektime-api/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]