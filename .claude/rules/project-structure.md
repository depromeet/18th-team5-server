# 프로젝트 구조

## 패키지 구조 (도메인 기반)
```
src/main/java/com/team/peektime_api/
├── PeektimeApiApplication.java   # 메인 클래스
├── domain/                       # 도메인별 패키지
│   └── [도메인명]/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       └── dto/
├── global/                       # 공통 설정
│   ├── config/                   # 설정 클래스
│   ├── exception/                # 예외 처리
│   └── common/                   # 공통 응답, 유틸
└── infra/                        # 외부 연동
```

## 설정 파일
- `application.yml`: 기본 설정
- `application-local.yml`: 로컬 개발 설정 (gitignore)
- `application-prod.yml`: 운영 설정