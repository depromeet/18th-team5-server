# PeekTime 시스템 아키텍처

## 목차
1. [개요](#개요)
2. [시스템 구조](#시스템-구조)
3. [서버별 역할](#서버별-역할)
4. [데이터베이스 설계](#데이터베이스-설계)
5. [API 통신](#api-통신)
6. [데이터 흐름](#데이터-흐름)

---

## 개요

PeekTime은 **Admin Server**와 **PeekTime Server** 두 개의 서버로 구성됩니다.

**핵심 원칙**
- 미션 정의(생성/수정/삭제)는 Admin Server에서만 관리 → **입구 하나**
- 미션 수행(할당/완료)은 PeekTime Server에서 독립적으로 관리
- 각 서버는 독립적인 DB를 가짐

---

## 시스템 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                            [Admin Web]                                      │
│                           (관리자 전용)                                      │
│                                │                                            │
│                                ▼                                            │
│  ┌─────────────────────────────────────────────────────────┐               │
│  │                      Admin Server                        │               │
│  │                                                          │               │
│  │   • LLM 미션 생성                                        │               │
│  │   • 미션 선별 및 관리 (CRUD)                              │               │
│  │   • 절기별 미션 배정                                      │               │
│  │   • Internal API 제공                                    │               │
│  │                                                          │               │
│  └─────────────────────┬────────────────────────────────────┘               │
│                        │                                                    │
│                        ▼                                                    │
│                   [Admin DB]                                                │
│                   • missions                                                │
│                   • seasons                                                 │
│                   • mission_type_assignments                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                         │
                         │ Internal API
                         │ (미션 데이터 조회)
                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│  ┌─────────────────────────────────────────────────────────┐               │
│  │                    PeekTime Server                       │               │
│  │                                                          │               │
│  │   • 사용자 온보딩 (타입 결정)                             │               │
│  │   • 미션 조회 (Admin API 호출 + 캐싱)                     │               │
│  │   • 사용자별 미션 할당                                    │               │
│  │   • 미션 완료 처리                                       │               │
│  │   • 푸시 알림                                            │               │
│  │                                                          │               │
│  └─────────────────────┬────────────────────────────────────┘               │
│                        │                                                    │
│                        ▼                                                    │
│                  [PeekTime DB]                                              │
│                   • users                                                   │
│                   • user_types                                              │
│                   • user_missions                                           │
│                                                                             │
│                        │                                                    │
│                        ▼                                                    │
│                   [User App]                                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 서버별 역할

### Admin Server

| 기능 | 설명 |
|------|------|
| LLM 미션 생성 | 절기 7일 전 배치로 미션 60개 생성 |
| 미션 CRUD | 미션 생성, 수정, 삭제, 조회 |
| 미션 선별 | LLM 생성 미션 중 사용할 미션 선택 |
| 타입별 배정 | 6개 사용자 타입에 미션 배정 |
| Internal API | PeekTime Server에 미션 데이터 제공 |

**접근 권한**: 내부망, 관리자만 접근

### PeekTime Server

| 기능 | 설명 |
|------|------|
| 사용자 온보딩 | 5개 질문으로 사용자 타입 결정 |
| 미션 조회 | Admin API에서 미션 가져오기 (캐싱) |
| 미션 할당 | 사용자 타입에 맞는 미션 3개 할당 |
| 미션 완료 처리 | 사용자의 미션 완료 상태 관리 |
| 푸시 알림 | 절기 전날 미션 도착 알림 |

**접근 권한**: 외부망, 일반 사용자 접근

---

## 데이터베이스 설계

### Admin DB

```sql
-- 절기 정보
seasons (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),           -- 예: "입춘"
    description TEXT,           -- 절기 설명
    start_date DATE,            -- 절기 시작일
    created_at TIMESTAMP
)

-- 미션 마스터
missions (
    id BIGINT PRIMARY KEY,
    season_id BIGINT,           -- FK: seasons
    content TEXT,               -- 미션 내용
    tags VARCHAR(100),          -- 예: "#야외 #힐링"
    status VARCHAR(20),         -- DRAFT, APPROVED, ARCHIVED
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)

-- 미션-타입 배정
mission_type_assignments (
    id BIGINT PRIMARY KEY,
    mission_id BIGINT,          -- FK: missions
    user_type VARCHAR(20),      -- 예: "OUTDOOR_HEALING"
    created_at TIMESTAMP
)
```

### PeekTime DB

```sql
-- 사용자
users (
    id BIGINT PRIMARY KEY,
    nickname VARCHAR(100),
    user_type VARCHAR(20),      -- 예: "OUTDOOR_HEALING"
    push_token VARCHAR(255),
    created_at TIMESTAMP
)

-- 사용자별 미션
user_missions (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,             -- FK: users
    mission_id BIGINT,          -- Admin DB의 mission ID (FK 아님)
    season_id BIGINT,           -- Admin DB의 season ID (FK 아님)
    mission_content TEXT,       -- 미션 내용 (캐시용 복사본)
    status VARCHAR(20),         -- ASSIGNED, COMPLETED, SKIPPED
    assigned_at TIMESTAMP,
    completed_at TIMESTAMP
)
```

**참고**: `user_missions.mission_id`는 Admin DB의 PK를 값으로 저장하지만, FK 제약조건은 없음 (DB가 분리되어 있으므로)

---

## API 통신

### Admin → PeekTime (Internal API)

```
Base URL: http://admin-server:8080/api/internal
```

#### 절기별 미션 조회
```
GET /api/internal/missions?seasonId={seasonId}&userType={userType}

Response:
{
    "seasonId": 1,
    "seasonName": "입춘",
    "missions": [
        {
            "missionId": 101,
            "content": "동네 공원에서 봄 기운 느끼며 산책하기",
            "tags": ["야외", "힐링"]
        },
        ...
    ]
}
```

#### 현재 절기 조회
```
GET /api/internal/seasons/current

Response:
{
    "seasonId": 1,
    "name": "입춘",
    "startDate": "2025-02-04",
    "description": "봄의 시작"
}
```

#### 특정 미션 조회
```
GET /api/internal/missions/{missionId}

Response:
{
    "missionId": 101,
    "content": "동네 공원에서 봄 기운 느끼며 산책하기",
    "tags": ["야외", "힐링"],
    "seasonId": 1,
    "seasonName": "입춘"
}
```

---

## 데이터 흐름

### 1. 미션 생성 및 배정 (Admin)

```
D-7: LLM 미션 생성
┌─────────────────────────────────────────────────────────┐
│ [배치 스케줄러] → [LLM API] → [missions 테이블 저장]     │
└─────────────────────────────────────────────────────────┘

D-5 ~ D-3: 관리자 검수 및 배정
┌─────────────────────────────────────────────────────────┐
│ [Admin Web] → [미션 선별] → [타입별 배정] → [DB 저장]    │
└─────────────────────────────────────────────────────────┘
```

### 2. 미션 할당 (PeekTime)

```
D-1: 사용자별 미션 할당 (자정 배치)
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  1. 활성 사용자 전체 조회                                │
│                    │                                    │
│                    ▼                                    │
│  2. Admin API 호출: 현재 절기 + 타입별 미션 조회         │
│                    │                                    │
│                    ▼                                    │
│  3. 사용자별 미션 3개 랜덤 선택                          │
│                    │                                    │
│                    ▼                                    │
│  4. user_missions 테이블 저장                           │
│                    │                                    │
│                    ▼                                    │
│  5. 푸시 알림 발송                                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 3. 미션 완료 처리 (PeekTime)

```
사용자 미션 완료 요청
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  [User App] → [PeekTime API] → [user_missions UPDATE]   │
│                                                         │
│  * Admin Server 호출 없음 (독립적 처리)                  │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 4. 미션 목록 조회 (PeekTime)

```
사용자 앱에서 미션 조회
┌─────────────────────────────────────────────────────────┐
│                                                         │
│  [User App]                                             │
│      │                                                  │
│      ▼                                                  │
│  [PeekTime API]                                         │
│      │                                                  │
│      ├─→ user_missions 조회 (할당된 미션 ID, 상태)       │
│      │                                                  │
│      └─→ 미션 상세는 캐시에서 조회                       │
│          (캐시 미스 시 Admin API 호출)                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 캐싱 전략

### 미션 데이터 캐싱 (PeekTime)

미션 마스터 데이터는 자주 변경되지 않으므로 적극적으로 캐싱

```
캐시 키: mission:{missionId}
캐시 키: season:{seasonId}:missions:{userType}
TTL: 24시간 (절기 변경 시 무효화)

갱신 시점:
- PeekTime 서버 시작 시
- 절기 변경 시 (D-1 배치에서)
- 캐시 미스 시
```

### 장애 대응

```
Admin Server 장애 시:
├─ 캐시된 미션 데이터로 서비스 유지
├─ 이미 할당된 미션은 정상 제공
└─ 미션 완료 처리 정상 동작

PeekTime Server 장애 시:
├─ Admin Server는 영향 없음
└─ 관리자 미션 관리 작업 정상 진행
```

---

## 기술 스택

| 구분 | Admin Server | PeekTime Server |
|------|--------------|-----------------|
| Framework | Spring Boot 4.0 | Spring Boot 4.0 |
| Language | Java 25 | Java 25 |
| Database | MySQL | MySQL |
| Cache | - | Redis (미션 캐싱) |
| API Docs | SpringDoc OpenAPI | SpringDoc OpenAPI |

---

## 향후 확장 고려사항

1. **이벤트 기반 동기화**: Admin에서 미션 배정 완료 시 이벤트 발행 → PeekTime에서 구독하여 캐시 갱신
2. **API Gateway**: 두 서버 앞에 Gateway 추가하여 인증/라우팅 통합
3. **모니터링**: 서버 간 API 호출 지연 시간 모니터링