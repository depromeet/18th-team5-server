# 조인 + 커버링 인덱스 실험 — NLJ 랜덤 I/O 단계별 제거

## 해결해야 할 쿼리 (같은 유저 타입 미션 랭킹 TOP 20)

```sql
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;
```

> **관찰 포인트**: EXPLAIN은 드리븐 rows≈2 (전체 평균 통계),
> EXPLAIN ANALYZE 실측은 rows≈10 loops≈50000 — 추정의 5배.

## 데이터 배경 — 편중(skew)

NATURE_OUTDOOR 유형의 멤버는 미션을 다른 타입에 비해서 많이 한 상황 (데이터 편중화).

| 항목 | 예측 (EXPLAIN) | 실측 (EXPLAIN ANALYZE) |
|---|---|---|
| NATURE_OUTDOOR 필터 통과 | 10% | 2% |
| 멤버당 미션 수행 횟수 | rows = 2 | rows = 10 |

---

## 0단계 — 초기 실행계획

```
-> Limit: 20 row(s)  (actual time=2401..2401 rows=20 loops=1)
    -> Sort: mission_count DESC, limit input to 20 row(s) per chunk  (actual time=2401..2401 rows=20 loops=1)
        -> Table scan on <temporary>  (actual time=2396..2399 rows=49993 loops=1)
            -> Aggregate using temporary table  (actual time=2396..2396 rows=49993 loops=1)
                -> Nested loop inner join  (cost=997546 rows=670433) (actual time=3.01..2242 rows=503936 loops=1)
                    -> Filter: (u.user_type = 'NATURE_OUTDOOR')  (cost=260095 rows=248593) (actual time=2.61..375 rows=50000 loops=1)
                        -> Table scan on u  (cost=260095 rows=2.49e+6) (actual time=2.6..310 rows=2.5e+6 loops=1)
                    -> Index lookup on l using idx_user_id (user_id=u.id)  (cost=2.7 rows=2.7) (actual time=0.0152..0.037 rows=10.1 loops=50000)
```

**시간 분해:**

| 구간 | 시간 |
|---|---|
| users 풀스캔 | 310ms |
| 조인 50,000회 반복 | 2242 - 310 = **1932ms** |
| 임시 테이블 집계 (GROUP BY 49,993그룹) | 154ms |
| Top-N 정렬 | 3ms |

**EXPLAIN 결과:**

- type: `ALL`
- Extra: `Using where`, `Using temporary`, `Using filesort` (필터링, 임시 테이블, 정렬 비용)

---

## 1단계 — 선행 테이블 users 풀스캔 제거

```sql
CREATE INDEX idx_users_type ON users (user_type);
```

**결과**: users 테이블 풀스캔 제거 — **310ms → 85.3ms** 단축. EXPLAIN(실행계획)에서 `Using where`는 제거했지만, Index lookup 수행 후 테이블 룩업은 제거하지 못했다 → 랜덤 I/O는 추가적으로 발생.

**단서:**

```
-> Index lookup on u using idx_users_type (user_type='NATURE_OUTDOOR')  (cost=44130 rows=96378) (actual time=1.87..85.3 rows=50000 loops=1)
```

**실행계획:**

```
-> Limit: 20 row(s)  (actual time=2033..2033 rows=20 loops=1)
    -> Sort: mission_count DESC, limit input to 20 row(s) per chunk  (actual time=2033..2033 rows=20 loops=1)
        -> Table scan on <temporary>  (actual time=2028..2031 rows=49993 loops=1)
            -> Aggregate using temporary table  (actual time=2028..2028 rows=49993 loops=1)
                -> Nested loop inner join  (cost=324533 rows=259922) (actual time=2.03..1874 rows=503936 loops=1)
                    -> Index lookup on u using idx_users_type (user_type='NATURE_OUTDOOR')  (cost=44130 rows=96378) (actual time=1.87..85.3 rows=50000 loops=1)
                    -> Index lookup on l using idx_user_id (user_id=u.id)  (cost=2.64 rows=2.7) (actual time=0.0145..0.0354 rows=10.1 loops=50000)
```

---

## 2단계 — 커버링 인덱스로 users 테이블(클러스터드 인덱스) 접근 제거

```sql
CREATE INDEX idx_type_name ON users (user_type, nickname);
```

**결과**: users 테이블 조회 시간 **85.3ms → 11ms** 단축. EXPLAIN에서 `Using index` 확인 → users 테이블에서 커버링 인덱스 정상 동작 확인.

**단서** — EXPLAIN ANALYZE에서:

```
-> Covering index lookup on u using idx_type_name (user_type='NATURE_OUTDOOR')  (cost=14424 rows=103024) (actual time=0.2..11 rows=50000 loops=1)
```

**실행계획:**

```
-> Limit: 20 row(s)  (actual time=1879..1879 rows=20 loops=1)
    -> Sort: mission_count DESC, limit input to 20 row(s) per chunk  (actual time=1879..1879 rows=20 loops=1)
        -> Table scan on <temporary>  (actual time=1874..1878 rows=49993 loops=1)
            -> Aggregate using temporary table  (actual time=1874..1874 rows=49993 loops=1)
                -> Nested loop inner join  (cost=311898 rows=277846) (actual time=0.667..1726 rows=503936 loops=1)
                    -> Covering index lookup on u using idx_type_name (user_type='NATURE_OUTDOOR')  (cost=14424 rows=103024) (actual time=0.2..11 rows=50000 loops=1)
                    -> Index lookup on l using idx_user_id (user_id=u.id)  (cost=2.62 rows=2.7) (actual time=0.014..0.034 rows=10.1 loops=50000)
```

---

## 3단계 — 후행 테이블(user_mission_log) 조인 랜덤 I/O 제거

위의 EXPLAIN ANALYZE를 보면, 여전히 50,000번의 조인이 반복적으로 발생 — 조인 시간: 1726 - 11 = **1715ms** 여전히 발생.

EXPLAIN 결과에서 user_mission_log 테이블은 이미 `idx_user_id` 인덱스를 잘 타고 있다 (type = `ref`).

**인덱스는 잘 타지만 조인이 1.715초 발생하는 이유:**

세컨더리 인덱스 `idx_user_id`의 리프에는 (user_id, PK인 id)까지만 있어서, `MAX(l.created_at)`을 계산하려면 실제 user_mission_log 테이블(클러스터드 인덱스)을 한 번 더 찾아가야 created_at을 읽을 수 있다. **이 두 번째 점프가 곧 랜덤 I/O가 발생하는 랜덤 액세스이다.**

우리의 쿼리에서 NATURE_OUTDOOR 타입의 유저는 평균 10개의 미션을 수행했다.

즉, **loops=50,000번 × 1인당 10건 = 총 50만 번의 랜덤 액세스**가 발생한다.

user_mission_log 테이블에서 조인 조건으로 사용되는 컬럼은 user_id이고, MAX로 사용되는 컬럼은 created_at이다.

**(user_id, created_at) 복합 인덱스를 만들어보자.** user_id를 통해 조인 탐색을 하고, created_at 값까지 세컨더리 인덱스의 리프 노드에서 읽을 수 있다.

```sql
CREATE INDEX idx_id_created_at_ ON user_mission_log (user_id, created_at);
```

**결과**: 총 쿼리 수행시간 **2401ms → 303ms** 단축 (Nested loop join 시간 격감: 1726ms → 149ms).

EXPLAIN 시 `Using index` 확인 (users, user_mission_log 양쪽) → 커버링 인덱스 정상 동작 (랜덤 I/O 제거) → 테이블 룩업 제거.

**실행계획:**

```
-> Limit: 20 row(s)  (actual time=303..303 rows=20 loops=1)
    -> Sort: mission_count DESC, limit input to 20 row(s) per chunk  (actual time=303..303 rows=20 loops=1)
        -> Table scan on <temporary>  (actual time=299..302 rows=49993 loops=1)
            -> Aggregate using temporary table  (actual time=299..299 rows=49993 loops=1)
                -> Nested loop inner join  (cost=144818 rows=268850) (actual time=0.599..149 rows=503936 loops=1)
                    -> Covering index lookup on u using idx_type_name (user_type='NATURE_OUTDOOR')  (cost=14424 rows=103024) (actual time=0.548..11.7 rows=50000 loops=1)
                    -> Covering index lookup on l using idx_id_created_at_ (user_id=u.id)  (cost=1 rows=2.61) (actual time=0.00129..0.00237 rows=10.1 loops=50000)
```

---

## 4단계 — 임시 테이블 집계 제거 (스트리밍 집계)

3단계 EXPLAIN ANALYZE를 다시 확인해보자.

```
-> Limit: 20 row(s)  (actual time=303..303 rows=20 loops=1)
    -> Sort: mission_count DESC, limit input to 20 row(s) per chunk  (actual time=303..303 rows=20 loops=1)
        -> Table scan on <temporary>  (actual time=299..302 rows=49993 loops=1)
            -> Aggregate using temporary table  (actual time=299..299 rows=49993 loops=1)
                -> Nested loop inner join  (cost=144818 rows=268850) (actual time=0.599..149 rows=503936 loops=1)
                    -> Covering index lookup on u using idx_type_name (user_type='NATURE_OUTDOOR')  (cost=14424 rows=103024) (actual time=0.548..11.7 rows=50000 loops=1)
                    -> Covering index lookup on l using idx_id_created_at_ (user_id=u.id)  (cost=1 rows=2.61) (actual time=0.00129..0.00237 rows=10.1 loops=50000)
```

**시간 분해:**

| 구간 | 시간 |
|---|---|
| Nested loop inner join | 149ms |
| Aggregate using temporary table | 299 - 149 = **150ms** |
| Table scan on \<temporary\> + Sort | 4ms |

위의 결과를 보면 **약 50%가 임시 테이블 집계**에 사용된다.

GROUP BY를 할 때 행이 **키 순서로 들어오면** MySQL은 임시 테이블 없이 인덱스 순서만으로 그룹을 묶는다. 인덱스 정렬 순서가 GROUP BY 키와 어긋나면, 임시 테이블에 모아놓고 그룹핑할 수밖에 없다.

**현재 상태:**

```sql
CREATE INDEX idx_type_name ON users (user_type, nickname);
CREATE INDEX idx_id_created_at_ ON user_mission_log (user_id, created_at);
```

현재 GROUP BY는 users 테이블의 `id, nickname`으로 그룹핑을 한다. 그럼 GROUP BY 순서에 맞게 선행 컬럼을 id → nickname 순으로 맞춰야 할까? 기존의 user_type은 인덱스 순서를 어떻게 둬야 할까?

인덱스를 `(user_type, id, nickname)` 순으로 만들면 — id, nickname은 user_type 선행 컬럼을 **기준으로** 정렬되어 있다. 하지만 우리의 쿼리는 WHERE가 user_type을 `'NATURE_OUTDOOR'` **상수로 고정**한다. 이 쿼리가 읽는 구간은 전부 NATURE_OUTDOOR 타입이다.

즉, 인덱스 `(user_type, id, nickname)`은 쿼리 안에서 **"id, nickname 순서로 정렬된 NATURE_OUTDOOR 명단"처럼 동작**한다. 상수 user_type을 걷어낸 나머지가 GROUP BY 키 순서(`id, nickname`)와 정확히 일치한다.

인덱스를 다시 만들어보자.

```sql
CREATE INDEX idx_type_id_nickname ON users (user_type, id, nickname);
```

**실행계획:**

```
-> Limit: 20 row(s)  (actual time=245..245 rows=20 loops=1)
    -> Sort: mission_count DESC, limit input to 20 row(s) per chunk  (actual time=245..245 rows=20 loops=1)
        -> Stream results  (cost=176746 rows=79045) (actual time=0.373..242 rows=49993 loops=1)
            -> Group aggregate: count(l.id), max(l.created_at)  (cost=176746 rows=79045) (actual time=0.361..231 rows=49993 loops=1)
                -> Nested loop inner join  (cost=149072 rows=276747) (actual time=0.335..167 rows=503936 loops=1)
                    -> Covering index lookup on u using idx_type_id_nickname (user_type='NATURE_OUTDOOR')  (cost=14848 rows=106050) (actual time=0.235..10.8 rows=50000 loops=1)
                    -> Covering index lookup on l using idx_id_created_at_ (user_id=u.id)  (cost=1 rows=2.61) (actual time=0.00159..0.00274 rows=10.1 loops=50000)
```

**결과:**

- 3단계에서는 `Aggregate using temporary table`(임시 테이블 집계)였다. 하지만 지금은 **`Group aggregate` + `Stream results`** 로 바뀌었다. 임시 테이블을 훑던 `Table scan on <temporary>` 줄도 통째로 사라졌다.
- 스트리밍 집계가 성공한 것이다. 행이 id, nickname 순서로 도착하므로, MySQL은 한 회원의 미션 기록이 지나가는 즉시 — 그 그룹이 끝나면 — COUNT, MAX를 확정할 수 있다. **임시 테이블에 5만 그룹의 행을 쌓았다가 다시 읽는 왕복이 사라졌다.**
- 상태 변수로도 확인: 쿼리 실행 시 `Created_tmp_tables` 증가량이 1 → 0.
- 집계 구간: **150ms → 64ms** 단축 (Group aggregate: 231 - 167 = 64ms)
- 총 실행 시간: **2401ms → 245ms, 약 10배 단축**

### 남은 filesort는 거의 공짜다

`ORDER BY mission_count DESC`는 `COUNT(l.id) AS mission_count` — **집계가 끝나야 비로소 존재하는 값**을 정렬한다. 테이블 어디에도 저장돼 있지 않으니, 어떤 인덱스로도 미리 정렬해 둘 수 없다. 따라서 집계가 나온 다음에 정렬할 수밖에 없다.

하지만 다행히 정렬 부담이 적다. `ORDER BY ... LIMIT 20`이라 상위 20개만 유지하는 **Top-N 정렬(우선순위 큐)** 을 사용한다. EXPLAIN ANALYZE에서도 Sort 구간은 245 - 242 = **3ms**에 불과하다.

GROUP BY 결과를 다시 정렬해야 한다면 filesort를 피하기 어렵지만, **그 비용이 작으면 문제가 안 된다.**

---

## 단계별 요약

| 단계 | 조치 | 총 수행시간 | 개선 지점 |
|---|---|---|---|
| 0 | (초기 상태) | 2401ms | — |
| 1 | `idx_users_type (user_type)` | 2033ms | users 풀스캔 310ms → 85.3ms |
| 2 | `idx_type_name (user_type, nickname)` | 1879ms | users 커버링: 85.3ms → 11ms |
| 3 | `idx_id_created_at_ (user_id, created_at)` | 303ms | 드리븐 커버링: NLJ 1726ms → 149ms (랜덤 액세스 50만 회 제거) |
| 4 | `idx_type_id_nickname (user_type, id, nickname)` | **245ms** | 스트리밍 집계: 임시 테이블 제거, 집계 150ms → 64ms |