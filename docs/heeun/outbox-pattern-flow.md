# Outbox 패턴 전체 플로우

## 개요

미션 완료 시 API DB에 기록을 저장하고, Admin 서버에 통계용 로그를 전송하는 전체 흐름입니다.
트랜잭션 아웃박스 패턴을 사용하여 데이터 정합성을 보장합니다.

---

## 전체 플로우 (비동기 + 폴링)

```mermaid
sequenceDiagram
    autonumber
    participant Service as completeMission()
    participant Outbox as OutboxRecorder
    participant MySQL
    participant Relay as AfterCommitRelay
    participant Admin as Admin Server
    participant Poller as Poller (Fallback)

    Service->>Outbox: record(payload)
    Outbox->>MySQL: INSERT outbox_event (READY)
    Outbox-->>Service: Spring Event (OutboxSavedEvent)
    Service->>MySQL: saveUserMissionCompletion()

    Note over MySQL: TX COMMIT

    rect rgb(144, 238, 144)
        Note right of Relay: ⚡ Happy Path — 지연 0ms
        Relay->>Admin: adminClient.sendMissionLog()

        alt [Admin 정상]
            Admin-->>Relay: 200 OK
            Relay->>MySQL: deleteOutbox() [REQUIRES_NEW]
        else [Admin 장애 / Timeout]
            Note over Relay: 예외 삼킴, READY 유지
        end
    end

    rect rgb(255, 200, 150)
        Note right of Poller: 🔄 Fallback — Admin 장애 시에만 동작

        Poller->>MySQL: READY 조회 (SKIP LOCKED)
        MySQL-->>Poller: OutboxEvent 목록

        Poller->>Admin: sendMissionLog()

        alt [성공 / 멱등]
            Admin-->>Poller: 200 OK
            Poller->>MySQL: deleteOutbox()
        else [409 Conflict]
            Note over Poller: TransientFailure → 재시도 대기
        else [4xx 기타]
            Note over Poller: PermanentFailure → 삭제
        end
    end
```

---

## 멱등성 처리 플로우

```mermaid
sequenceDiagram
    autonumber
    participant Relay as AfterCommitRelay
    participant Poller as Poller (Fallback)
    participant Admin as Admin Server
    participant AdminDB as Admin DB

    rect rgb(255, 220, 180)
        Note over Relay,Admin: 첫 번째 요청 (성공했으나 응답 손실)
        Relay->>Admin: POST /mission-log
        Admin->>AdminDB: 멱등성 키 체크 → 없음
        Admin->>AdminDB: INSERT 로그
        Admin--xRelay: 200 OK (응답 손실!)
        Note over Relay: 예외 삼킴, Outbox 유지
    end

    rect rgb(144, 238, 144)
        Note over Poller,AdminDB: 폴러 재시도 (멱등성 보장)
        Poller->>Admin: POST /mission-log (동일 요청)
        Admin->>AdminDB: 멱등성 키 체크 → 존재함
        Note over Admin: 저장 안 함 (이미 처리됨)
        Admin-->>Poller: 200 OK (ALREADY_EXISTS)
        Poller->>Poller: Outbox 삭제
    end
```

---

## 동시 요청 처리 (Race Condition)

```mermaid
sequenceDiagram
    autonumber
    participant Relay as AfterCommitRelay
    participant Poller as Poller (Fallback)
    participant Admin as Admin Server
    participant AdminDB as Admin DB

    par 동시 전송
        Relay->>Admin: POST /mission-log
    and
        Poller->>Admin: POST /mission-log
    end

    Admin->>AdminDB: 멱등성 키 체크 (둘 다 없음)

    par 동시 INSERT
        Admin->>AdminDB: INSERT (Relay)
    and
        Admin->>AdminDB: INSERT (Poller)
    end

    Note over AdminDB: UNIQUE 제약 위반!

    Admin-->>Relay: 200 OK (성공)
    Admin-->>Poller: 409 Conflict (충돌)

    Note over Relay: Success → Outbox 삭제
    Note over Poller: TransientFailure → 재시도 대기

    rect rgb(144, 238, 144)
        Note over Poller,AdminDB: 다음 폴링 주기
        Poller->>Admin: POST /mission-log (재시도)
        Admin->>AdminDB: 멱등성 키 존재 확인
        Admin-->>Poller: 200 OK (ALREADY_EXISTS)
        Note over Poller: Success → Outbox 삭제
    end
```

---

## 전체 아키텍처

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
