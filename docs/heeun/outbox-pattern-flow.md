# Outbox 패턴 전체 플로우

## 개요

미션 완료 시 API DB에 기록을 저장하고, Admin 서버에 통계용 로그를 전송하는 전체 흐름입니다.
트랜잭션 아웃박스 패턴을 사용하여 데이터 정합성을 보장합니다.

---

## 1. 정상 플로우 (즉시 전송 성공)

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Controller
    participant Service
    participant DB as API DB
    participant EventListener
    participant AdminClient
    participant Admin as Admin Server

    Client->>Controller: POST /missions/{id}/complete
    Controller->>Service: completeMission()

    rect rgb(230, 245, 255)
        Note over Service,DB: @Transactional
        Service->>DB: UserMissionCompletion 저장
        Service->>DB: OutboxEvent 저장
        Service->>Service: 이벤트 발행
        Service-->>DB: COMMIT
    end

    Service-->>Controller: 응답 생성
    Controller-->>Client: 200 OK

    rect rgb(255, 245, 230)
        Note over EventListener,Admin: @Async + AFTER_COMMIT (비동기)
        EventListener->>AdminClient: sendMissionLog()
        AdminClient->>Admin: POST /api/stats/mission-log
        Admin-->>AdminClient: 200 OK (저장 완료)
        AdminClient-->>EventListener: Success
        EventListener->>DB: Outbox 삭제
    end
```

---

## 2. 비동기 전송 실패 → 폴러 재시도

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant Service
    participant DB as API DB
    participant EventListener
    participant AdminClient
    participant Admin as Admin Server
    participant Poller

    Client->>Service: completeMission()

    rect rgb(230, 245, 255)
        Note over Service,DB: @Transactional
        Service->>DB: UserMissionCompletion 저장
        Service->>DB: OutboxEvent 저장 (status=READY)
        Service-->>DB: COMMIT
    end

    Service-->>Client: 200 OK

    rect rgb(255, 230, 230)
        Note over EventListener,Admin: 비동기 전송 실패
        EventListener->>AdminClient: sendMissionLog()
        AdminClient->>Admin: POST /api/stats/mission-log
        Admin--xAdminClient: ⏱️ Timeout / 네트워크 오류
        AdminClient-->>EventListener: TransientFailure
        Note over EventListener: Outbox 유지 (삭제 안 함)
    end

    rect rgb(230, 255, 230)
        Note over Poller,Admin: 30초 후 폴러 재시도
        Poller->>DB: READY 상태 Outbox 조회
        DB-->>Poller: OutboxEvent 목록
        Poller->>AdminClient: sendMissionLog()
        AdminClient->>Admin: POST /api/stats/mission-log
        Admin-->>AdminClient: 200 OK
        AdminClient-->>Poller: Success
        Poller->>DB: Outbox 삭제
    end
```

---

## 3. 멱등성 처리 (중복 요청)

```mermaid
sequenceDiagram
    autonumber
    participant EventListener
    participant Poller
    participant AdminClient
    participant Admin as Admin Server
    participant AdminDB as Admin DB

    rect rgb(255, 245, 230)
        Note over EventListener,Admin: 첫 번째 요청 (성공, 응답 손실)
        EventListener->>AdminClient: sendMissionLog()
        AdminClient->>Admin: POST /mission-log
        Admin->>AdminDB: 멱등성 키 체크 → 없음
        Admin->>AdminDB: 로그 저장
        Admin--xAdminClient: 200 OK (응답 손실!)
        Note over EventListener: Outbox 유지됨
    end

    rect rgb(230, 255, 230)
        Note over Poller,AdminDB: 폴러 재시도 (멱등성 처리)
        Poller->>AdminClient: sendMissionLog()
        AdminClient->>Admin: POST /mission-log (동일 요청)
        Admin->>AdminDB: 멱등성 키 체크 → 존재함
        Note over Admin: 저장 안 함 (이미 처리됨)
        Admin-->>AdminClient: 200 OK (ALREADY_EXISTS)
        AdminClient-->>Poller: Success
        Poller->>Poller: Outbox 삭제
    end
```

---

## 4. 동시 요청 처리 (Race Condition)

```mermaid
sequenceDiagram
    autonumber
    participant EventListener
    participant Poller
    participant AdminClient
    participant Admin as Admin Server
    participant AdminDB as Admin DB

    par 동시 전송
        EventListener->>AdminClient: sendMissionLog()
        AdminClient->>Admin: POST /mission-log
    and
        Poller->>AdminClient: sendMissionLog()
        AdminClient->>Admin: POST /mission-log
    end

    Admin->>AdminDB: 멱등성 키 체크 (둘 다 없음)

    par 동시 INSERT 시도
        Admin->>AdminDB: INSERT (EventListener 요청)
    and
        Admin->>AdminDB: INSERT (Poller 요청)
    end

    Note over AdminDB: UNIQUE 제약 위반!

    AdminDB-->>Admin: 하나만 성공, 하나는 실패

    Admin-->>AdminClient: 200 OK (성공한 요청)
    Admin-->>AdminClient: 409 Conflict (실패한 요청)

    Note over EventListener: Success → Outbox 삭제
    Note over Poller: TransientFailure → 재시도 대기

    rect rgb(230, 255, 230)
        Note over Poller,AdminDB: 다음 폴링 시
        Poller->>Admin: POST /mission-log (재시도)
        Admin->>AdminDB: 멱등성 키 체크 → 존재함
        Admin-->>Poller: 200 OK (ALREADY_EXISTS)
        Note over Poller: Success → Outbox 삭제
    end
```

---

## 5. 전체 아키텍처

```mermaid
flowchart TB
    subgraph Client
        APP[Mobile App]
    end

    subgraph API["API Server"]
        CTRL[Controller]
        SVC[Service]
        REPO[Repository]
        LISTENER[EventListener<br/>@Async]
        POLLER[OutboxPoller<br/>@Scheduled 30s]
        CLIENT[AdminClient]
    end

    subgraph APIDB["API Database"]
        COMPLETION[(UserMissionCompletion)]
        OUTBOX[(OutboxEvent)]
    end

    subgraph Admin["Admin Server"]
        STATS[StatsService]
    end

    subgraph AdminDB["Admin Database"]
        LOG[(UserMissionLog<br/>+ idempotency_key)]
    end

    APP -->|1. POST /complete| CTRL
    CTRL --> SVC

    SVC -->|2. 트랜잭션| REPO
    REPO -->|저장| COMPLETION
    REPO -->|저장| OUTBOX

    SVC -.->|3. 이벤트 발행| LISTENER
    LISTENER -->|4. 즉시 전송| CLIENT

    POLLER -->|5. 주기적 조회| OUTBOX
    POLLER -->|6. 재시도| CLIENT

    CLIENT -->|HTTP| STATS
    STATS -->|저장| LOG

    style OUTBOX fill:#fff3cd
    style LOG fill:#d4edda
```

---

## 6. 상태 흐름

```mermaid
stateDiagram-v2
    [*] --> READY: Outbox 저장

    READY --> 즉시전송: EventListener
    즉시전송 --> [*]: 성공 → 삭제
    즉시전송 --> READY: 실패 → 유지

    READY --> 폴러재시도: 30초 후
    폴러재시도 --> [*]: 성공 → 삭제
    폴러재시도 --> READY: 일시실패 → 재시도
    폴러재시도 --> [*]: 영구실패 → 삭제
```

---

## 7. 응답 코드별 처리

```mermaid
flowchart LR
    subgraph Admin응답
        R200[200 OK<br/>성공/멱등]
        R409[409 Conflict<br/>동시처리중]
        R4XX[4XX 기타<br/>클라이언트에러]
        R5XX[5XX<br/>서버에러]
        TIMEOUT[Timeout<br/>네트워크]
    end

    subgraph API처리
        SUCCESS[Success]
        TRANSIENT[TransientFailure]
        PERMANENT[PermanentFailure]
    end

    subgraph Outbox
        DELETE[삭제]
        KEEP[유지<br/>재시도]
    end

    R200 --> SUCCESS --> DELETE
    R409 --> TRANSIENT --> KEEP
    R4XX --> PERMANENT --> DELETE
    R5XX --> TRANSIENT --> KEEP
    TIMEOUT --> TRANSIENT --> KEEP

    style DELETE fill:#d4edda
    style KEEP fill:#fff3cd
```

---

## 요약

| 단계 | 설명 | 동기/비동기 |
|-----|------|-----------|
| 1. 미션 완료 저장 | UserMissionCompletion + OutboxEvent | 동기 (트랜잭션) |
| 2. 즉시 전송 | EventListener → Admin | 비동기 (@Async) |
| 3. 폴러 재시도 | 30초마다 READY 상태 조회 | 비동기 (@Scheduled) |
| 4. 멱등성 처리 | Admin에서 중복 요청 무시 | - |

### 핵심 보장

- **원자성**: 미션 완료 + Outbox 저장이 하나의 트랜잭션
- **최소 1회 전송 (At-Least-Once)**: 실패 시 폴러가 재시도
- **중복 방지**: Admin의 멱등성 키로 중복 저장 차단
- **사용자 경험**: 비동기 처리로 응답 지연 없음
