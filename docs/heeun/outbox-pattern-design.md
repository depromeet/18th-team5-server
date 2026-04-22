# 미션 완료 통계 기록 시스템 설계

## 1. 개요

### 목표
- 사용자 미션 완료 시 peektime-admin의 통계 테이블에 기록
- 분산 환경에서 데이터 정합성 보장
- 장애 상황에서도 데이터 유실 없이 처리

### 적용 개념
| 개념 | 설명 |
|------|------|
| **멱등성 (Idempotency)** | 같은 요청을 여러 번 보내도 결과가 동일 |
| **트랜잭션 분리** | 로컬 DB 저장과 외부 API 호출을 분리 |
| **재시도 (Retry)** | 실패한 요청을 스케줄러로 재처리 |
| **Outbox Pattern** | 이벤트를 로컬 DB에 저장 후 비동기 전송 |

---

## 2. 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        peektime-api                         │
│                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────┐ │
│  │   Mission   │    │   Outbox    │    │   Scheduler     │ │
│  │   Service   │───▶│   Event     │◀───│  (5분 주기)     │ │
│  └─────────────┘    └─────────────┘    └────────┬────────┘ │
│         │                                       │          │
│         │ 같은 트랜잭션                          │ 재시도    │
│         ▼                                       ▼          │
│  ┌─────────────┐                        ┌─────────────┐    │
│  │ User Mission│                        │  HTTP Call  │    │
│  │ Completion  │                        │  to Admin   │    │
│  └─────────────┘                        └──────┬──────┘    │
│                                                │           │
└────────────────────────────────────────────────┼───────────┘
                                                 │
                                                 ▼
┌─────────────────────────────────────────────────────────────┐
│                       peektime-admin                        │
│                                                             │
│  ┌─────────────────┐    ┌─────────────────────────────┐    │
│  │  Stats API      │───▶│  MissionCompletionStats     │    │
│  │  (멱등성 체크)   │    │  (통계 테이블)               │    │
│  └─────────────────┘    └─────────────────────────────┘    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 2.1 핵심 개념: 트랜잭션 분리

### Outbox Pattern의 핵심
**"로컬 DB 저장"과 "외부 API 호출"을 분리한다.**

```
┌─────────────────────────────────────────────────────────────┐
│                    하나의 트랜잭션 (@Transactional)          │
│                                                             │
│   ┌─────────────────────────┐   ┌─────────────────────────┐│
│   │ 1. UserMissionCompletion│   │ 2. OutboxEvent          ││
│   │    저장                 │   │    저장 (PENDING)       ││
│   │                         │   │                         ││
│   │ - userId                │   │ - idempotencyKey (UUID) ││
│   │ - missionId             │   │ - eventType             ││
│   │ - missionType           │   │ - payload (JSON)        ││
│   │ - completedAt           │   │ - status: PENDING       ││
│   └─────────────────────────┘   └─────────────────────────┘│
│                                                             │
│   → 둘 다 성공하거나, 둘 다 롤백됨 (원자성 보장)              │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 트랜잭션 커밋 완료
                              │ (사용자에게 성공 응답 반환)
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              트랜잭션 밖 (비동기 / 스케줄러)                  │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │ 3. Admin API 호출 (통계 기록)                        │  │
│   │                                                     │  │
│   │ - 성공: OutboxEvent 상태 → SENT                     │  │
│   │ - 실패: 상태 유지, 스케줄러가 나중에 재시도           │  │
│   └─────────────────────────────────────────────────────┘  │
│                                                             │
│   → Admin 장애가 나도 사용자 미션 완료는 이미 저장됨         │
│   → 데이터 유실 없음, 나중에 재시도 가능                    │
└─────────────────────────────────────────────────────────────┘
```

### 코드 예시 (기본 구조)
```java
@Service
@RequiredArgsConstructor
public class MissionCompletionService {

    private final UserMissionCompletionRepository completionRepository;
    private final OutboxEventRepository outboxRepository;

    @Transactional  // 하나의 트랜잭션
    public MissionCompletionResult completeMission(Long userId, Long missionId, MissionType type) {

        // 1. 사용자 미션 완료 기록 저장
        UserMissionCompletion completion = UserMissionCompletion.builder()
            .userId(userId)
            .missionId(missionId)
            .missionType(type)
            .completedAt(LocalDateTime.now())
            .build();
        completionRepository.save(completion);

        // 2. Outbox 이벤트 저장 (같은 트랜잭션)
        OutboxEvent event = OutboxEvent.builder()
            .idempotencyKey(UUID.randomUUID().toString())
            .eventType("MISSION_COMPLETED")
            .payload(toJson(completion))  // JSON 변환
            .status(OutboxStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .build();
        outboxRepository.save(event);

        // 3. 결과 반환 (Admin 호출은 트랜잭션 밖에서)
        return MissionCompletionResult.of(completion, event);
    }
}
```

---

## 2.2 트랜잭션 커밋 후 Admin 호출 방식 (선택 필요)

Admin API 호출은 **트랜잭션 커밋 후**에 해야 안전합니다. 두 가지 방식 중 선택:

### 옵션 A: `@TransactionalEventListener(AFTER_COMMIT)`

스프링 이벤트 기반으로 트랜잭션 커밋 후 자동 실행

```java
// 1. 서비스에서 이벤트 발행
@Service
@RequiredArgsConstructor
public class MissionCompletionService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MissionCompletionResponse completeMission(Long userId, Long missionId, MissionType type) {
        // ... 저장 로직 ...

        completionRepository.save(completion);
        outboxRepository.save(event);

        // 이벤트 발행 (아직 트랜잭션 안)
        eventPublisher.publishEvent(new MissionCompletedEvent(event.getId()));

        return MissionCompletionResponse.of(completion);
    }
}

// 2. 리스너에서 커밋 후 처리
@Component
@RequiredArgsConstructor
public class MissionCompletedEventListener {

    private final AdminStatsClient adminClient;
    private final OutboxEventRepository outboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionCompleted(MissionCompletedEvent event) {
        // 트랜잭션 커밋 후에 실행됨
        try {
            adminClient.recordStats(event.getOutboxEventId());
            // 성공 시 상태 업데이트
        } catch (Exception e) {
            // 실패해도 OK, 스케줄러가 재시도
            log.warn("Admin 즉시 호출 실패, 스케줄러가 재시도 예정", e);
        }
    }
}
```

### 옵션 B: Facade 패턴

명시적으로 트랜잭션 경계를 분리

```java
// 1. Facade (트랜잭션 없음)
@Component
@RequiredArgsConstructor
public class MissionCompletionFacade {

    private final MissionCompletionService service;
    private final AdminStatsClient adminClient;

    public MissionCompletionResponse complete(Long userId, Long missionId, MissionType type) {
        // Step 1: 트랜잭션 처리 (저장)
        MissionCompletionResult result = service.completeMission(userId, missionId, type);

        // Step 2: 트랜잭션 커밋 완료 후 Admin 호출
        try {
            adminClient.recordStats(result.getOutboxEvent());
            // 성공 시 Outbox 상태 업데이트
        } catch (Exception e) {
            // 실패해도 OK, 스케줄러가 재시도
            log.warn("Admin 즉시 호출 실패, 스케줄러가 재시도 예정", e);
        }

        return result.getResponse();
    }
}

// 2. 컨트롤러에서 Facade 호출
@RestController
@RequiredArgsConstructor
public class MissionController {

    private final MissionCompletionFacade facade;  // Service 대신 Facade

    @PostMapping("/api/missions/{missionId}/complete")
    public SuccessResponse<MissionCompletionResponse> complete(...) {
        return SuccessResponse.ok(facade.complete(userId, missionId, type));
    }
}
```

### 비교

| 항목 | 옵션 A (AFTER_COMMIT) | 옵션 B (Facade) |
|------|----------------------|-----------------|
| **코드 흐름** | 암묵적 (이벤트 기반) | 명시적 (순차 호출) |
| **디버깅** | 이벤트 추적 필요 | 스택트레이스 명확 |
| **테스트** | 이벤트 발행 모킹 필요 | 단순 메서드 호출 |
| **레이어** | 기존 구조 유지 | Facade 레이어 추가 |
| **스프링 의존성** | ApplicationEventPublisher 사용 | 없음 |
| **확장성** | 여러 리스너 추가 용이 | 명시적으로 추가해야 함 |

### 결정 필요 ⚠️
- [ ] 옵션 A (AFTER_COMMIT) 선택
- [ ] 옵션 B (Facade) 선택

### 왜 이렇게 하는가?

| 문제 상황 | 기존 방식 (직접 호출) | Outbox Pattern |
|----------|----------------------|----------------|
| Admin 서버 다운 | 미션 완료 실패 (500 에러) | 미션 완료 성공, 나중에 재시도 |
| 네트워크 장애 | 미션 완료 실패 | 미션 완료 성공, 나중에 재시도 |
| 타임아웃 | 미션 완료 불확실 | 미션 완료 성공, 확실한 재시도 |
| 중복 호출 | 통계 중복 기록 가능 | 멱등성 키로 중복 방지 |

---

## 3. 상세 플로우

### 3.1 미션 완료 플로우

```
1. 사용자가 미션 완료 요청
   POST /api/missions/{missionId}/complete

2. peektime-api 트랜잭션 시작
   ┌────────────────────────────────────────┐
   │ 2-1. UserMissionCompletion 저장        │
   │ 2-2. OutboxEvent 저장 (PENDING)        │
   │      - idempotencyKey: UUID 생성       │
   │      - eventType: MISSION_COMPLETED    │
   │      - payload: { missionId, userId,   │
   │                   missionType,         │
   │                   completedAt }        │
   └────────────────────────────────────────┘
   트랜잭션 커밋

3. 즉시 전송 시도 (optional, 최적화용)
   - 성공: OutboxEvent 상태 → SENT
   - 실패: 그대로 PENDING 유지 (스케줄러가 처리)

4. 사용자에게 성공 응답 반환
```

### 3.2 스케줄러 플로우

```
매 5분마다 실행:

1. PENDING 또는 FAILED 상태의 OutboxEvent 조회
   - retry_count < MAX_RETRY (5회)
   - created_at > now() - 7일 (오래된 건 제외)

2. 각 이벤트에 대해:
   ┌────────────────────────────────────────┐
   │ 2-1. peektime-admin API 호출           │
   │      POST /api/admin/stats/record      │
   │      Header: Idempotency-Key: {uuid}   │
   │                                        │
   │ 2-2. 성공 시:                          │
   │      - status → SENT                   │
   │      - processed_at 기록               │
   │                                        │
   │ 2-3. 실패 시:                          │
   │      - status → FAILED                 │
   │      - retry_count++                   │
   │      - last_error 기록                 │
   └────────────────────────────────────────┘

3. MAX_RETRY 초과 시:
   - status → DEAD_LETTER
   - 알림 발송 (슬랙, 로그 등)
```

### 3.3 Admin 통계 기록 플로우 (멱등성 처리)

```
POST /api/admin/stats/record
Header: Idempotency-Key: {uuid}

1. idempotency_key로 기존 처리 여부 확인
   SELECT * FROM processed_events
   WHERE idempotency_key = ?

2. 이미 처리됨:
   - 200 OK 반환 (멱등성)
   - 중복 기록하지 않음

3. 처음 처리:
   ┌────────────────────────────────────────┐
   │ 3-1. MissionCompletionStats 업데이트   │
   │      - 해당 미션의 completion_count++  │
   │                                        │
   │ 3-2. ProcessedEvent 저장               │
   │      - idempotency_key 기록            │
   │      - 중복 방지용                     │
   └────────────────────────────────────────┘

4. 200 OK 반환
```

---

## 4. 테이블 설계

### 4.1 peektime-api

#### OutboxEvent (이벤트 발행 대기열)
```java
@Entity
@Table(name = "outbox_event")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;  // UUID

    @Column(nullable = false)
    private String eventType;  // MISSION_COMPLETED

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;  // JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;  // PENDING, SENT, FAILED, DEAD_LETTER

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;
}
```

#### OutboxStatus (Enum)
```java
public enum OutboxStatus {
    PENDING("대기중", "이벤트 생성됨, 전송 대기"),
    SENT("전송완료", "admin에 성공적으로 전송됨"),
    FAILED("실패", "전송 실패, 재시도 대기"),
    DEAD_LETTER("최종실패", "최대 재시도 초과, 수동 처리 필요");
}
```

### 4.2 peektime-admin

#### ProcessedEvent (멱등성 체크용)
```java
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}
```

#### MissionCompletionStats (기존 테이블 활용)
```java
// 이미 존재하는 테이블
// missionType별로 completion_count 관리
```

---

## 5. API 설계

### 5.1 peektime-api (사용자용)

#### 미션 완료
```
POST /api/missions/{missionId}/complete

Request:
{
  "missionType": "DAILY"  // DAILY, RECOMMENDED, SELECTED
}

Response: 200 OK
{
  "code": "MISSION_200",
  "message": "미션 완료 성공",
  "result": {
    "missionId": 1,
    "completedAt": "2025-04-21T10:30:00"
  }
}
```

### 5.2 peektime-admin (내부 API)

#### 통계 기록
```
POST /api/admin/stats/record

Headers:
  Idempotency-Key: {uuid}

Request:
{
  "missionId": 1,
  "userId": 100,
  "missionType": "DAILY",
  "solarTermId": 5,
  "completedAt": "2025-04-21T10:30:00"
}

Response: 200 OK
{
  "code": "STATS_200",
  "message": "통계 기록 완료",
  "result": {
    "recorded": true,
    "duplicate": false
  }
}

Response (중복 요청): 200 OK
{
  "code": "STATS_200",
  "message": "통계 기록 완료",
  "result": {
    "recorded": false,
    "duplicate": true
  }
}
```

---

## 6. 재시도 전략

### 설정값
| 항목 | 값 | 설명 |
|------|-----|------|
| 스케줄러 주기 | 5분 | `@Scheduled(fixedRate = 300000)` |
| 최대 재시도 | 5회 | 초과 시 DEAD_LETTER |
| 이벤트 보관 | 7일 | 오래된 PENDING은 무시 |
| 배치 크기 | 100건 | 한 번에 처리할 최대 이벤트 수 |

### 재시도 간격 (Exponential Backoff 고려)
```
1차 실패: 즉시 FAILED, 5분 후 재시도
2차 실패: 10분 후 재시도
3차 실패: 20분 후 재시도
4차 실패: 40분 후 재시도
5차 실패: DEAD_LETTER 전환
```

---

## 7. 에러 처리

### 7.1 peektime-api 에러
| 상황 | 처리 |
|------|------|
| DB 저장 실패 | 트랜잭션 롤백, 500 에러 반환 |
| Admin 호출 실패 | Outbox PENDING 유지, 스케줄러가 재시도 |
| 이미 완료된 미션 | 409 Conflict 반환 |

### 7.2 peektime-admin 에러
| 상황 | 처리 |
|------|------|
| 중복 요청 (멱등성) | 200 OK 반환 (duplicate: true) |
| 미션 not found | 400 Bad Request |
| DB 저장 실패 | 500 에러, api가 재시도 |

---

## 8. 모니터링

### 확인할 지표
- PENDING 상태 이벤트 수 (너무 많으면 admin 장애 의심)
- FAILED 상태 이벤트 수
- DEAD_LETTER 이벤트 수 (알림 필요)
- 평균 처리 시간 (createdAt → processedAt)

### 알림 조건
- DEAD_LETTER 이벤트 발생 시
- PENDING 이벤트가 1시간 이상 체류 시
- 재시도 실패율이 50% 초과 시

---

## 9. 구현 순서

1. **peektime-api**
   - [ ] OutboxEvent 엔티티 생성
   - [ ] OutboxStatus enum 생성
   - [ ] OutboxEventRepository 생성
   - [ ] 미션 완료 서비스에 Outbox 저장 로직 추가
   - [ ] OutboxScheduler 구현 (재시도 스케줄러)
   - [ ] Admin API 호출 클라이언트 구현

2. **peektime-admin**
   - [ ] ProcessedEvent 엔티티 생성
   - [ ] 통계 기록 API 엔드포인트 생성
   - [ ] 멱등성 체크 로직 구현
   - [ ] MissionCompletionStats 업데이트 로직

3. **테스트**
   - [ ] 정상 플로우 테스트
   - [ ] 중복 요청 (멱등성) 테스트
   - [ ] Admin 장애 시 재시도 테스트
   - [ ] DEAD_LETTER 전환 테스트