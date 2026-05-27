# 분산락 개선: waitForReleaseIfFailed 옵션 추가

## 배경

OutboxPollerV3에서 Producer-Consumer 패턴 사용 시 문제 발생.

```
Producer: 분산락으로 하나의 인스턴스만 DB → Redis SET 장전
Consumer: 모든 인스턴스가 SPOP으로 병렬 처리
```

### 현재 문제

```
인스턴스 A                    인스턴스 B
─────────────────────────────────────────────
poll() 시작                   poll() 시작
  │                             │
  ▼                             ▼
Producer: 락 획득 ✓           Producer: 락 획득 실패 (waitTime=0)
  │                             │
  │ (장전 중... 5초)            ▼
  │                           Consumer: Redis SET 비어있음 ← 문제!
  │                             │
  ▼                             ▼
Producer: 장전 완료           아무것도 못 함
  │
  ▼
Consumer: 혼자서 처리
```

**인스턴스 B의 Consumer가 병렬 처리에 참여하지 못함.**

---

## 해결 방안

락 획득 실패 시 "락이 풀릴 때까지 대기"하는 옵션 추가.

### 원하는 동작

```
인스턴스 A                    인스턴스 B
─────────────────────────────────────────────
tryLock → 성공               tryLock → 실패
    │                            │
    ▼                            ▼
장전 중 (5초)                 락 해제 대기 중...
    │                            │
    ▼                            │
unlock()  ──────────────────→  대기 해제!
    │                            │
    ▼                            ▼
Consumer                      Consumer (장전 스킵)
    │                            │
    ▼                            ▼
         병렬 처리 참여!
```

---

## 코드 변경

### 1. DistributedLock 어노테이션

**현재:**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String key();
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    long waitTime() default 5L;
    long leaseTime() default 3L;
}
```

**변경 후:**
```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String key();
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    long waitTime() default 5L;
    long leaseTime() default 3L;

    /**
     * 락 획득 실패 시 락이 해제될 때까지 대기할지 여부
     * - false (기본값): 락 실패 시 바로 return false
     * - true: 락 실패 시 락 해제까지 대기 후 return false (메서드 실행 안 함)
     */
    boolean waitForReleaseIfFailed() default false;
}
```

---

### 2. DistributedLockAop

**현재:**
```java
@Around("@annotation(com.team.peektime_api.global.aop.DistributedLock)")
public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

    String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(
            signature.getParameterNames(),
            joinPoint.getArgs(),
            distributedLock.key()
    );
    RLock rLock = redissonClient.getLock(key);

    try {
        boolean available = rLock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
        );
        if (!available) {
            return false;
        }

        return aopForTransaction.proceed(joinPoint);
    } catch (InterruptedException e) {
        throw new InterruptedException();
    } finally {
        try {
            rLock.unlock();
        } catch (IllegalMonitorStateException e) {
            log.info("Redisson Lock Already UnLock serviceName={}, key={}", method.getName(), key);
        }
    }
}
```

**변경 후:**
```java
@Around("@annotation(com.team.peektime_api.global.aop.DistributedLock)")
public Object lock(final ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

    String key = REDISSON_LOCK_PREFIX + CustomSpringELParser.getDynamicValue(
            signature.getParameterNames(),
            joinPoint.getArgs(),
            distributedLock.key()
    );
    RLock rLock = redissonClient.getLock(key);

    try {
        boolean available = rLock.tryLock(
                distributedLock.waitTime(),
                distributedLock.leaseTime(),
                distributedLock.timeUnit()
        );

        if (!available) {
            if (!distributedLock.waitForReleaseIfFailed()) {
                // 기본 동작: 락 실패 시 바로 리턴
                return false;
            }

            // 락 해제 대기 모드: 락이 풀릴 때까지 대기 후 리턴
            log.info("락 획득 실패, 해제 대기 중: key={}", key);
            rLock.lock(distributedLock.leaseTime(), distributedLock.timeUnit());
            rLock.unlock();
            log.info("락 해제 감지, 진행: key={}", key);
            return false;
        }

        return aopForTransaction.proceed(joinPoint);
    } catch (InterruptedException e) {
        throw new InterruptedException();
    } finally {
        try {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        } catch (IllegalMonitorStateException e) {
            log.info("Redisson Lock Already UnLock serviceName={}, key={}", method.getName(), key);
        }
    }
}
```

**변경 포인트:**
1. `waitForReleaseIfFailed` 옵션 체크
2. `true`일 경우 `rLock.lock()` → `rLock.unlock()` → `return false`
3. `finally`에서 `isHeldByCurrentThread()` 체크 추가 (대기 모드에서 이미 unlock 했으므로)

---

### 3. OutboxProducer

**현재:**
```java
@DistributedLock(key = "'outbox:producer'", waitTime = 0, leaseTime = 10)
public boolean loadEventsToRedis() {
    // 장전 로직
}
```

**변경 후:**
```java
@DistributedLock(
    key = "'outbox:producer'",
    waitTime = 0,
    leaseTime = 10,
    waitForReleaseIfFailed = true
)
public boolean loadEventsToRedis() {
    // 장전 로직
}
```

---

## 동작 흐름 (변경 후)

```
인스턴스 A                              인스턴스 B
──────────────────────────────────────────────────────────
tryLock(0, 10, SECONDS) → 성공        tryLock(0, 10, SECONDS) → 실패
        │                                     │
        ▼                                     ▼
  메서드 실행 (장전)                    waitForReleaseIfFailed = true
        │                                     │
        │                                     ▼
        │                               lock(10, SECONDS) 블로킹...
        │                                     │
        ▼                                     │
  unlock() ────────────────────────────→ 락 획득!
        │                                     │
        ▼                                     ▼
  return true                           unlock() (바로 해제)
        │                                     │
        ▼                                     ▼
  Consumer 실행                         return false
        │                                     │
        ▼                                     ▼
              ←── 병렬 처리 ──→          Consumer 실행
```

---

## 옵션 요약

| 옵션 | 락 성공 | 락 실패 |
|------|---------|---------|
| `waitForReleaseIfFailed = false` (기본) | 메서드 실행 → return 결과 | 바로 return false |
| `waitForReleaseIfFailed = true` | 메서드 실행 → return 결과 | 락 해제 대기 → return false |

---

## 사용 케이스

### Producer-Consumer 패턴
- Producer: 하나의 인스턴스만 실행
- Consumer: 모든 인스턴스가 병렬 실행
- Producer 완료 후 Consumer가 동시에 시작되어야 함

```java
public void poll() {
    // Producer: 락 성공하면 장전, 실패하면 대기
    producer.loadEventsToRedis();

    // Consumer: 모든 인스턴스가 실행
    consumer.consumeAll();
}
```
