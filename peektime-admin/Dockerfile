# ---- Build Stage ----
FROM gradle:8-jdk25 AS builder
WORKDIR /app

# 루트부터 복사 (멀티모듈)
COPY . .

# admin 모듈만 빌드
RUN gradle :peektime-admin:bootJar -x test --no-daemon

# ---- Run Stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=builder /app/peektime-admin/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]