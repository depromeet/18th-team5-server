# PeekTime API

## 프로젝트 개요
PeekTime 서비스의 백엔드 API 서버

## 기술 스택
- Java 25
- Spring Boot 4.0.5
- Gradle 9.4.1
- Spring Data JPA
- Spring Security
- H2 (개발), MySQL (운영)
- Lombok
- SpringDoc OpenAPI (Swagger)

## 빌드 & 실행
```bash
./gradlew build      # 빌드
./gradlew bootRun    # 실행
./gradlew test       # 테스트
./gradlew clean build # 클린 빌드
```

## API 문서
- Swagger UI: http://localhost:8080/swagger-ui.html

## 관련 설계 문서
- 전역 공유 피드 / 캐시·동시성 작업 시 `docs/global-feed-design.md` 참조 (핵심 결정 D1~D4, 탐색 단계)