# 역정규화 카운터의 쓰기 경로 복잡도 — "lost update만 막으면 되는 거 아닌가?"

조인 커버링 실험(03-results.md)의 후속 질문.
랭킹 쿼리를 인덱스로 245ms까지 줄였는데, 애초에 `users.mission_count` 컬럼을
역정규화하면 조인·집계 자체가 사라지는 것 아닌가? 쓰기 경로가 복잡해진다는데
**그냥 count + 1의 lost update만 있는 것 아닌가?**

결론부터: **lost update는 역정규화 카운터의 문제 중 가장 쉬운 문제다.**
한 줄로 해결된다. 진짜 비용은 그 뒤에 있는 세 가지 — 멱등성, 쓰기 경로 전파, drift.

---

## 0. lost update — 맞다, 존재한다. 그리고 제일 쉽다

```java
// ❌ read-modify-write: 동시 요청 둘 다 count=5를 읽고 둘 다 6을 쓴다 → 1 증발
User user = userRepository.findById(userId).orElseThrow();
user.increaseMissionCount();   // 엔티티 필드 +1 후 더티체킹 flush
```

```java
// ✅ 원자적 UPDATE: DB가 행 락 안에서 현재값을 읽고 +1 — lost update 원천 차단
public interface UserRepository extends JpaRepository<User, Long> {

    @Modifying
    @Query("UPDATE User u SET u.missionCount = u.missionCount + 1 WHERE u.id = :userId")
    void incrementMissionCount(@Param("userId") Long userId);
}
```

`SET count = count + 1`은 DB가 읽기와 쓰기를 한 문장의 행 락 안에서 처리하므로
동시성 제어가 공짜다. **여기까지만 보면 역정규화는 정말 간단해 보인다.**
문제는 이 UPDATE가 "언제, 몇 번 실행되느냐"를 애플리케이션이 보장해야 한다는 것.

---

## 1. 진짜 문제 ① — 증가 연산은 멱등하지 않다

INSERT와 UPDATE +1의 결정적 차이:

| | 두 번 실행되면 | 멱등성 확보 수단 |
|---|---|---|
| `INSERT` (unique key) | 두 번째가 제약 위반으로 실패 | **스키마가 공짜로 보장** |
| `UPDATE count = count + 1` | 그냥 2가 더해진다 | **애플리케이션이 직접 보장해야 함** |

우리 프로젝트의 실제 코드가 정확히 이 구조다. `StatsService.saveMissionLog`는
멱등성 키 + unique 제약으로 재시도(outbox 재전송, 네트워크 타임아웃 후 재요청)를 막는다:

```java
// peektime-admin StatsService — 현재 코드 (INSERT만 있을 때)
public boolean saveMissionLog(MissionLogRequest request) {
    String idempotencyKey = request.idempotencyKey();

    if (userMissionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
        return false;                       // 재시도 → 아무 일도 안 일어남. 안전.
    }
    return saveIfAbsent(missionLog, idempotencyKey);
    // 레이스로 동시에 들어와도 unique 제약이 두 번째 insert를 막는다. 안전.
}
```

여기에 카운터를 "순진하게" 추가하면:

```java
// ❌ 카운터 추가 — 어디에 넣어도 구멍이 생긴다
public boolean saveMissionLog(MissionLogRequest request) {
    if (userMissionLogRepository.existsByIdempotencyKey(key)) {
        return false;
    }
    userMissionLogRepository.save(missionLog);        // (A) INSERT 성공
    userRepository.incrementMissionCount(userId);     // (B) +1
    return true;
}
```

장애 시나리오 (이 코드로는 막을 수 없다):

```
T1  (A) INSERT 성공 — 로그는 저장됨 (★ 이미 커밋 완료)
T2  (B) 실행 전에 서버 크래시 / 배포 재시작 / DB 커넥션 끊김
T3  outbox가 같은 이벤트 재전송 (at-least-once)
T4  existsByIdempotencyKey = true → return false → (B)는 영원히 실행 안 됨
```

**"T2에서 죽으면 (A)도 롤백되는 것 아닌가?" — 아니다, 이게 함정이다.**
롤백되려면 (A)와 (B)가 같은 트랜잭션 안에 있어야 하는데, 이 메서드에는
`@Transactional`이 없다. 그러면 `save()`는 Spring Data JPA의
`SimpleJpaRepository.save()` **자체 트랜잭션**으로 돌고, 리턴하는 순간
이미 커밋이 끝나 디스크에 영구 반영된 상태다. T2 시점에는 롤백할
트랜잭션이 이미 닫혀 있으므로 (A)는 사라지지 않는다.

결과: **로그 1건 존재, 카운터 +0. 그리고 이 오차는 영원히 남는다.**

(둘을 한 트랜잭션으로 묶으면 크래시 시 함께 롤백되고 재시도 시 함께 적용되어
이 틈은 닫힌다 — 그 대가가 바로 아래 2절의 트랜잭션 경계 재설계다.)
반대 순서((B) 먼저)로 바꾸면 "카운터 +1, 로그 없음 → 재시도로 로그 저장 + 또 +1"
→ 중복 증가. 순서를 어떻게 놓아도 둘 사이의 틈은 사라지지 않는다.

동시 요청의 `DataIntegrityViolationException` catch 분기도 마찬가지 —
"내 insert가 중복으로 실패했으면 +1도 하면 안 된다"를 분기마다 정확히
판단해야 한다. INSERT만 있을 때는 unique 제약이 알아서 하던 일이다.

---

## 2. 진짜 문제 ② — 트랜잭션으로 묶으면 되지 않나? 이 코드는 일부러 안 묶었다

틈을 없애는 정석은 (A)와 (B)를 한 트랜잭션으로 묶는 것이다. 그런데
`StatsService.saveMissionLog`에는 **의도적으로 `@Transactional`이 없다.**
코드 주석에 이유가 남아 있다:

> 트랜잭션 안에서 unique 제약 위반이 나면 그 시점에 트랜잭션이 rollback-only로
> 오염되어, 예외를 잡아 정상 리턴해도 커밋 시 UnexpectedRollbackException(500)이 나간다.

즉 카운터 하나를 넣으려면:

- 트랜잭션을 다시 도입하고, rollback-only 오염 문제를 다른 방법으로 재해결
  (중복을 catch가 아닌 사전 SELECT로만 거르기 — 레이스 창 재발, 또는
  `INSERT ... ON DUPLICATE KEY` 네이티브 쿼리로 전환, 또는 REQUIRES_NEW 분리 —
  분리하면 다시 원자성이 깨진다)

**컬럼 하나 추가가 "이미 한 번 신중하게 설계한 트랜잭션 경계를 처음부터 다시 푸는 일"이 된다.**
이것이 "쓰기 경로가 복잡해진다"의 구체적인 의미다. 읽기 쿼리에서 사라진 복잡도는
소멸한 게 아니라 쓰기 경로의 트랜잭션 설계로 이사했다.

---

## 3. 진짜 문제 ③ — 쓰는 곳이 한 군데가 아니다

카운터의 정합성은 "로그를 건드리는 **모든** 코드가 카운터도 같이 건드린다"는
약속 위에 서 있다. 이 약속은 컴파일러가 강제해 주지 않는다.

우리 프로젝트에서 로그/완료 기록을 건드리는 실제 경로들:

```java
// UserWithdrawalService — 탈퇴 유저 보존기간 경과 후 일괄 삭제
@Scheduled(cron = "0 0 3 * * *")
@Transactional
public void deleteWithdrawnUsers() {
    for (User user : withdrawnUsers) {
        userMissionCompletionRepository.deleteByUser_Id(userId);  // ← 카운터 -N 누가 하나?
        ...
    }
}
```

| 쓰기 경로 | 카운터에 필요한 일 | 잊으면 |
|---|---|---|
| 미션 완료 (3종: daily/recommended/selected) | +1 | 과소 집계 |
| 탈퇴 유저 삭제 배치 | 유저별 -N (어차피 유저 행도 지우니 이 경우는 무관하지만, soft delete였다면 필수) | 과대 집계 |
| 운영자 수동 데이터 보정 | 수동 재계산 | 조용한 drift |
| 과거 데이터 백필/마이그레이션 | 일괄 재계산 | 조용한 drift |
| **미래에 추가될 모든 경로** | 기억해야 함 | 조용한 drift |

INSERT 기반 집계(`COUNT(*)`)는 이 표 전체가 필요 없다 — **원본이 곧 진실이라
어떤 경로로 쓰든 읽기가 항상 맞다.** 카운터는 파생 데이터라서 모든 쓰기 경로가
동기화 의무를 진다.

---

## 4. 진짜 문제 ④ — drift는 자가 치유되지 않는다

인덱스와 카운터의 근본 차이:

- **인덱스**: 스토리지 엔진(InnoDB)이 행 변경과 **물리적으로 원자적으로** 유지.
  애플리케이션 버그로 인덱스가 테이블과 어긋나는 일은 (DB 버그가 아닌 한) 불가능.
  → 커버링 인덱스 해법에 정합성 리스크가 "없음"인 이유.
- **카운터**: 애플리케이션 코드가 유지. 위 1~3의 어느 구멍으로든 한 번 틀어지면
  **스스로 복구되지 않고, 이후의 모든 +1이 틀린 값 위에 쌓인다.**

그래서 실무의 역정규화 카운터에는 보정(reconciliation) 배치가 세트로 따라온다:

```sql
-- 주기 실행: 원본에서 재계산해 틀어진 카운터 교정
UPDATE users u
JOIN (
    SELECT user_id, COUNT(*) AS cnt
    FROM user_mission_log
    GROUP BY user_id
) t ON t.user_id = u.id
SET u.mission_count = t.cnt
WHERE u.mission_count <> t.cnt;
```

아이러니한 결론: **카운터를 유지하더라도, 원래 없애려던 그 GROUP BY 집계 쿼리를
보정 배치가 주기적으로 돌린다.** 읽기 핫패스에서 치우는 것이지 세상에서 없애는 게 아니다.

---

## 5. 그래서 판단 기준

| | ① 커버링 인덱스 (실험의 해법) | ② 역정규화 카운터 |
|---|---|---|
| 읽기 | 245ms (집계는 매번 수행) | ~1ms (LIMIT 행만 읽음) |
| 쓰기 | 변화 없음 (인덱스 유지비용뿐) | 모든 쓰기 경로 수정 + 트랜잭션 재설계 |
| 정합성 | 리스크 없음 (엔진이 보장) | 멱등성·전파·drift 전부 애플리케이션 책임 |
| 부대 비용 | 없음 | 보정 배치 상시 운영 |
| 롤백 | `DROP INDEX` 한 줄 | 컬럼 제거 + 코드 원복 |

- 소비처가 **어드민 통계/랭킹 화면** → 245ms로 충분. ①에서 멈춘다. (실험의 결론)
- 소비처가 **유저 대면 핫패스** (높은 QPS로 매 요청 집계) → ②로 격상.
  단, users 행을 직접 넓히기보다 별도 통계 테이블·배치 집계·Redis sorted set을
  먼저 검토 — 배치 집계는 멱등성 문제 자체가 사라진다 (매번 원본에서 재계산하므로).

**한 줄 요약: lost update는 `SET count = count + 1` 한 줄로 끝나는 가장 쉬운 문제고,
역정규화의 진짜 비용은 ①증가 연산이 멱등하지 않아 재시도·장애 틈에서 틀어지고
②그걸 막으려면 트랜잭션 경계를 재설계해야 하며 ③모든 쓰기 경로가 동기화 의무를 지고
④한 번 틀어지면 자가 치유가 안 돼 보정 배치까지 운영해야 한다는 것이다.**