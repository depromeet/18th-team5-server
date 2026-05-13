# 미션 완료 기록 Flow 설계 문서

## 개요

사용자가 미션을 완료하면 API 서버에 기록을 저장하고, Admin 서버에 통계용 로그를 전송하는 전체 흐름을 정리한 문서입니다.

---

## 시스템 구성

```
┌─────────────────┐         ┌─────────────────┐
│   peektime-api  │  ────►  │  peektime-admin │
│   (API 서버)     │  HTTP   │   (Admin 서버)   │
└─────────────────┘         └─────────────────┘
        │                           │
        ▼                           ▼
   ┌─────────┐                ┌─────────┐
   │ API DB  │                │ Admin DB│
   └─────────┘                └─────────┘
```

- **peektime-api**: 사용자 대면 API 서버 (미션 완료 기록 저장)
- **peektime-admin**: 관리자 서버 (통계용 미션 로그 저장)

---

## 문제 정의

### 왜 단순 HTTP 호출로는 안 되는가?

미션 완료 시 두 가지 작업이 필요합니다:
1. **API DB에 UserMissionCompletion 저장**
2. **Admin 서버에 HTTP로 미션 로그 전송**

#### 문제 상황 1: DB 저장 후 HTTP 전송 실패

```
[API 서버]
1. UserMissionCompletion 저장 ✅ (DB 커밋됨)
2. Admin 서버로 HTTP 전송 ❌ (네트워크 장애)
   → 사용자에게 500 에러 반환

결과: API DB에는 기록이 있지만, Admin에는 로그가 없음 → 데이터 불일치
```

#### 문제 상황 2: HTTP 전송 성공 후 DB 저장 실패

```
[API 서버]
1. Admin 서버로 HTTP 전송 ✅
2. UserMissionCompletion 저장 ❌ (DB 장애)
   → 트랜잭션 롤백

결과: Admin에는 로그가 있지만, API DB에는 기록이 없음 → 데이터 불일치
```

#### 문제 상황 3: HTTP 응답 타임아웃

```
[API 서버]
1. UserMissionCompletion 저장 ✅
2. Admin 서버로 HTTP 전송... (응답 대기 중 타임아웃)
   → 재시도하면 Admin에 중복 저장될 수 있음

결과: Admin에 동일 로그가 2번 저장될 수 있음 → 중복 데이터
```

### 핵심 문제

> **DB 트랜잭션과 HTTP 호출은 원자적(atomic)으로 처리될 수 없다.**

- DB 트랜잭션: 롤백 가능
- HTTP 호출: 한번 전송되면 롤백 불가

둘을 하나의 트랜잭션처럼 묶을 수 없기 때문에, 반드시 **데이터 정합성 보장 전략**이 필요합니다.

---

## 해결책: 트랜잭션 아웃박스 패턴 (Transactional Outbox Pattern)

### 핵심 아이디어

1. **HTTP 전송 대신, Outbox 테이블에 이벤트를 저장한다** (같은 DB 트랜잭션 내)
2. **트랜잭션 커밋 후, 비동기로 HTTP 전송을 시도한다**
3. **실패하면 폴러(Poller)가 주기적으로 재시도한다**
4. **수신 측에서 멱등성 키로 중복을 방지한다**

### 왜 원자적인가?

```java
@Transactional
public void completeMission(...) {
    // 1. 미션 완료 기록 저장
    userMissionCompletionRepository.save(completion);

    // 2. Outbox 이벤트 저장 (같은 트랜잭션!)
    outboxRepository.save(new OutboxEvent(payload));

    // → 둘 다 같은 트랜잭션이므로, 둘 다 성공하거나 둘 다 롤백됨
}
```

- 미션 완료 + Outbox 저장은 **같은 DB 트랜잭션**
- HTTP 전송은 **트랜잭션 커밋 후** 별도로 수행
- HTTP 실패해도 Outbox에 데이터가 남아있으므로 **재시도 가능**

---

## 설계한 Flow

### 전체 시퀀스 다이어그램

```
┌──────┐       ┌──────────┐       ┌─────────┐       ┌───────────┐       ┌─────────┐
│Client│       │Controller│       │ Service │       │EventListener│     │AdminClient│
└──┬───┘       └────┬─────┘       └────┬────┘       └─────┬─────┘       └────┬────┘
   │                │                  │                  │                  │
   │ POST /complete │                  │                  │                  │
   │───────────────►│                  │                  │                  │
   │                │ completeMission()│                  │                  │
   │                │─────────────────►│                  │                  │
   │                │                  │                  │                  │
   │                │    ┌─────────────┴─────────────┐    │                  │
   │                │    │ @Transactional            │    │                  │
   │                │    │ 1. 중복 체크               │    │                  │
   │                │    │ 2. UserMissionCompletion  │    │                  │
   │                │    │    저장                   │    │                  │
   │                │    │ 3. OutboxEvent 저장       │    │                  │
   │                │    │ 4. 이벤트 발행            │    │                  │
   │                │    └─────────────┬─────────────┘    │                  │
   │                │                  │                  │                  │
   │                │                  │ ── 트랜잭션 커밋 ──│                  │
   │                │                  │                  │                  │
   │                │◄─────────────────│                  │                  │
   │◄───────────────│ 200 OK          │                  │                  │
   │                │                  │                  │                  │
   │                │                  │  @Async          │                  │
   │                │                  │  AFTER_COMMIT    │                  │
   │                │                  │─────────────────►│                  │
   │                │                  │                  │ sendMissionLog() │
   │                │                  │                  │─────────────────►│
   │                │                  │                  │                  │──► Admin 서버
   │                │                  │                  │                  │
   │                │                  │                  │  성공 시:         │
   │                │                  │                  │  Outbox 삭제     │
   │                │                  │                  │◄─────────────────│
   │                │                  │                  │                  │
   │                │                  │                  │  실패 시:         │
   │                │                  │                  │  로그만 남김      │
   │                │                  │                  │  (Poller가 재시도)│
```

### 상세 Flow

#### Step 1: API 요청 수신

```
POST /api/v1/missions/{missionId}/complete
```

**파일**: `UserMissionCompletionController.java`

```java
@PostMapping("/{missionId}/complete")
public SuccessResponse<UserMissionCompletionResponse> completeMission(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long missionId,
        @RequestBody @Valid UserMissionCompletionRequest request
) {
    return SuccessResponse.of(SuccessCode.MISSION_COMPLETED,
            userMissionCompletionService.completeMission(principal.getUserId(), missionId, request));
}
```

#### Step 2: 트랜잭션 내 처리 (원자적)

**파일**: `UserMissionCompletionService.java`

```java
@Transactional
public UserMissionCompletionResponse completeMission(Long userId, Long missionId, UserMissionCompletionRequest request) {
    User user = findUser(userId);

    // 1. 중복 완료 체크
    if (userMissionCompletionRepository.existsByUser_IdAndMissionId(user.getId(), missionId)) {
        throw new BusinessException(ErrorCode.MISSION_ALREADY_COMPLETED);
    }

    // 2. 미션 완료 기록 저장
    UserMissionCompletion completion = userMissionCompletionRepository.save(
            UserMissionCompletion.of(user, missionId, request)
    );

    // 3. Outbox 이벤트 저장 (같은 트랜잭션!)
    MissionLogPayload payload = createMissionLogPayload(missionId, request, user, completion);
    OutboxEvent outbox = outboxRepository.save(new OutboxEvent(toJson(payload)));

    // 4. 스프링 이벤트 발행 (트랜잭션 커밋 후 처리됨)
    eventPublisher.publishEvent(MissionCompletedEvent.from(outbox, payload));

    return UserMissionCompletionResponse.from(completion);
}
```

**핵심 포인트**:
- `UserMissionCompletion` 저장과 `OutboxEvent` 저장이 **같은 트랜잭션**
- 둘 중 하나라도 실패하면 **둘 다 롤백**
- 이벤트 발행은 트랜잭션 내에서 하지만, 처리는 커밋 후

#### Step 3: 트랜잭션 커밋 후 비동기 전송

**파일**: `MissionCompletedEventListener.java`

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Async
public void handle(MissionCompletedEvent event) {
    try {
        MissionLogPayload payload = MissionLogPayload.from(event);
        adminClient.sendMissionLog(payload);

        // 성공하면 Outbox에서 삭제
        outboxRepository.deleteById(event.outboxId());

        log.info("미션 완료 로그 전송 성공: missionId={}", event.missionId());
    } catch (Exception e) {
        // 실패해도 괜찮음 - Poller가 재시도할 것
        log.warn("미션 완료 로그 즉시 전송 실패, 폴러가 재시도 예정: {}", e.getMessage());
    }
}
```

**핵심 포인트**:
- `@TransactionalEventListener(phase = AFTER_COMMIT)`: 트랜잭션 커밋 후에만 실행
- `@Async`: 별도 스레드에서 비동기 실행 (사용자 응답 지연 없음)
- 실패해도 예외를 던지지 않음 → Outbox에 데이터가 남아있음

#### Step 4: Admin 서버로 HTTP 전송

**파일**: `AdminClient.java`

```java
public void sendMissionLog(MissionLogPayload payload) {
    AdminApiResponse response = adminRestClient.post()
            .uri("/api/stats/mission-log")
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(AdminApiResponse.class);
}
```

#### Step 5: Admin 서버에서 로그 저장 (멱등성 보장)

**파일**: `StatsService.java` (peektime-admin)

```java
@Transactional
public void saveMissionLog(MissionLogRequest request) {
    LocalDate completedDate = request.completedAt().toLocalDate();

    // 멱등성 키 생성: userUuid:missionId:completedDate:hash
    String idempotencyKey = generateIdempotencyKey(request.userUuid(), request.missionId(), completedDate);

    // 이미 존재하면 무시 (중복 방지)
    if (userMissionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
        log.info("이미 존재하는 미션 로그, 무시: idempotencyKey={}", idempotencyKey);
        return;
    }

    // 로그 저장
    userMissionLogRepository.save(createMissionLogBy(request, idempotencyKey, completedDate));
}
```

**멱등성 키 구조**:
```
{userUuid}:{missionId}:{completedDate}:{sha256_hash_8자리}
```

예: `abc123:42:2024-01-15:a1b2c3d4`

#### Step 6: 실패 시 Poller가 재시도

**파일**: `OutboxPoller.java`

```java
@Scheduled(fixedDelay = 60000)  // 1분마다
public void pollAndProcess() {
    // 생성된 지 3초 이상 된 것만 조회 (즉시 처리 중인 것 제외)
    LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
    List<OutboxEvent> events = outboxRepository.findByCreatedAtBefore(threshold);

    for (OutboxEvent event : events) {
        try {
            MissionLogPayload payload = objectMapper.readValue(event.getPayload(), MissionLogPayload.class);
            adminClient.sendMissionLog(payload);
            outboxRepository.delete(event);  // 성공하면 삭제
        } catch (Exception e) {
            log.error("Outbox 재시도 실패: id={}", event.getId());
            // 다음 폴링 때 다시 시도
        }
    }
}
```

---

## 데이터 모델

### API 서버 (peektime-api)

#### OutboxEvent

```java
@Entity
@Table(name = "outbox_event")
public class OutboxEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;  // JSON 형태의 MissionLogPayload
}
```

#### UserMissionCompletion

| 컬럼 | 설명 |
|------|------|
| id | PK |
| user_id | 사용자 FK |
| mission_id | 미션 ID |
| object_key | S3 이미지 키 |
| completed_at | 완료 시각 |

### Admin 서버 (peektime-admin)

#### UserMissionLog

| 컬럼 | 설명 |
|------|------|
| id | PK |
| idempotency_key | 멱등성 키 (UNIQUE) |
| user_uuid | 사용자 UUID |
| mission_id | 미션 ID |
| mission_type | DAILY/RECOMMENDED/SELECTED |
| solar_term_id | 절기 ID |
| completed_date | 완료 날짜 |
| completed_at | 완료 시각 |

---

## 정합성 보장 메커니즘

### 1. 트랜잭션 원자성

| 상황 | 결과 |
|------|------|
| DB 저장 성공 + Outbox 저장 성공 | 트랜잭션 커밋 ✅ |
| DB 저장 실패 | 전체 롤백 ↩️ |
| Outbox 저장 실패 | 전체 롤백 ↩️ |

### 2. 최소 1회 전송 보장 (At-Least-Once)

```
즉시 전송 시도 (EventListener)
        │
        ▼
   ┌─────────┐
   │ 성공?   │
   └────┬────┘
        │
   ┌────┴────┐
   │         │
  Yes       No
   │         │
   ▼         ▼
Outbox    Outbox에
삭제      데이터 유지
          │
          ▼
     Poller가
     재시도
```

### 3. 중복 처리 방지 (멱등성)

Admin 서버에서 멱등성 키로 중복 체크:

```java
if (userMissionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
    return;  // 이미 존재하면 무시
}
```

---

## 장애 시나리오별 동작

### 시나리오 1: Admin 서버 일시 장애

```
1. API 서버: 미션 완료 저장 ✅, Outbox 저장 ✅
2. EventListener: Admin 전송 시도 → 실패 ❌
3. Outbox에 데이터 유지
4. 1분 후 Poller 재시도 → Admin 복구됨 → 성공 ✅
5. Outbox 삭제

결과: 정합성 유지 ✅
```

### 시나리오 2: API 서버 재시작

```
1. API 서버: 미션 완료 저장 ✅, Outbox 저장 ✅
2. EventListener 실행 전 서버 재시작
3. Outbox에 데이터 유지
4. 서버 재시작 후 Poller가 미전송 건 발견 → 전송 ✅

결과: 정합성 유지 ✅
```

### 시나리오 3: 네트워크 타임아웃 후 중복 전송

```
1. EventListener: Admin 전송 → 타임아웃 (실제로는 성공했을 수 있음)
2. Poller: 재시도 → Admin에 도착
3. Admin: 멱등성 키 확인 → 이미 존재 → 무시

결과: 중복 방지 ✅
```

---

## 요약

| 구성 요소 | 역할 |
|----------|------|
| **@Transactional** | 미션 완료 + Outbox 저장을 원자적으로 처리 |
| **OutboxEvent** | 전송 대기 중인 이벤트 저장 |
| **@TransactionalEventListener** | 트랜잭션 커밋 후 즉시 전송 시도 |
| **@Async** | 사용자 응답 지연 없이 비동기 전송 |
| **OutboxPoller** | 실패한 이벤트 주기적 재시도 |
| **멱등성 키** | Admin 서버에서 중복 저장 방지 |

### 트랜잭션 아웃박스 패턴을 사용해야 하는 이유

1. **원자성 보장**: DB 저장과 이벤트 저장이 하나의 트랜잭션
2. **신뢰성**: 네트워크 장애에도 데이터 유실 없음
3. **최종 일관성**: 언젠가는 반드시 Admin에 전송됨
4. **중복 방지**: 멱등성 키로 중복 처리 차단
5. **사용자 경험**: 비동기 처리로 응답 지연 없음