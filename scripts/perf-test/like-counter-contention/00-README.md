# 좋아요 카운터 행 락 경합 실험 (단일 행 vs 슬롯)

## 가설

전역 피드 좋아요 수를 `user_mission_completion.like_count` **단일 컬럼**에 `+1` 하면,
인기 피드 1건에 좋아요가 몰릴 때 모든 UPDATE가 **그 한 행의 행 락에서 직렬화**된다.
동시성이 올라갈수록 처리량은 천장을 치고 p99는 치솟으며, InnoDB 행 락 대기(`Innodb_row_lock_waits`)가
눈에 띄게 쌓인다. → 나중에 슬롯 카운터(`INSERT ... ON DUPLICATE KEY UPDATE count=count+1`, 랜덤 슬롯)로
바꿨을 때 이 경합이 사라지는지 A/B로 비교한다.

## 왜 앱 레벨(B안)인가

순수 DB 세션으로도 재현되지만, 여기선 **운영과 동일한 코드 경로**(`FeedLikeService.like`)를 그대로
타면서 1000 TPS를 주고 싶어 HTTP 부하로 간다. 핵심은 **서로 다른 유저**로 때리는 것:
`FeedLike`의 `UNIQUE(user_id, completion_id)` + "멤버십이 새로 생길 때만 카운터 +1" 설계 때문에,
**같은 유저로 때리면 첫 요청만 INSERT되고 나머지는 멱등 무변화 → 카운터 UPDATE가 안 일어나** 경합이
0이 된다. 그래서 부하 생성기가 매 요청 **유일한 userId**를 보낸다(k6 `iterationInTest`).

## 구성 요소

| 파일 | 역할 |
|------|------|
| `01-seed.sql` | 핫 피드 항목 1건(완료, `object_key` 채움) 적재 + 재실행용 리셋 |
| `02-k6-contention.js` | k6 부하 — 고정 completion에 유일 userId로 `POST /loadtest/feed/{id}/like` |
| `03-observe.sql` | 관측 쿼리 — 행 락 상태 카운터 diff, 라이브 락 대기, 카운터 정합 검증 |
| `04-results.md` | 결과 기록 골격 (단일 행 baseline → 슬롯 A/B) |

## 사전 준비

1. **스키마**: 엔티티가 스키마 주인이다. 앱을 `local,loadtest` 프로필로 한 번 기동하면 로컬
   `local`은 `ddl-auto: update`라 `like_count` 컬럼과 `feed_like` 테이블이 자동 생성된다.
   (RDS 테스트 인스턴스를 쓸 거면 `local` 대신 `prod` — 단 앱과 seed가 같은 DB를 보게 맞출 것)
2. **부하 전용 엔드포인트**: `LoadTestFeedLikeController`는 `@Profile("loadtest")`라 이 프로필일 때만
   뜬다. 운영에선 빈 자체가 없어 `/loadtest/**`는 404.
3. **seed 적재** (앱과 **같은 DB**에서 — 로컬이면 localhost):
   ```bash
   mysql -h localhost -u heeunTest -p peaktime_db < 01-seed.sql
   ```
4. **k6 설치**: `brew install k6` (macOS).

## 실행

```bash
# 1) 앱 기동 (로컬 DB + 부하 전용 엔드포인트 활성화)
./gradlew :peektime-api:bootRun --args='--spring.profiles.active=local,loadtest'
#    → startup 로그 JDBC URL이 localhost:3306 인지 확인 (RDS 아님)
#    → local은 ddl-auto: update라 feed_like / like_count 자동 생성
#    (RDS 테스트 인스턴스에 돌리려면 local 대신 prod)

# 2) 관측 baseline 스냅샷 (다른 MySQL 세션)
#    03-observe.sql의 STEP 1 (상태 카운터 before) 실행

# 3) 부하 (1000 TPS, 60초)
k6 run 02-k6-contention.js
#    옵션 조절: RPS, DURATION, HOT_ID, VUS, MAX_VUS 환경변수
#    예) k6 run -e RPS=1500 -e DURATION=90s 02-k6-contention.js

# 4) 관측 after 스냅샷 + diff + 정합 검증 (03-observe.sql STEP 2~4)
```

## 관측 포인트 (무엇을 "눈으로" 보나)

- **k6**: `http_req_duration` p95/p99, `http_reqs`(실측 RPS), `dropped_iterations`
  (도착률을 못 맞췄다 = 경합 천장). 동시성이 오를수록 p99가 비선형으로 튄다.
- **InnoDB 행 락**: `Innodb_row_lock_waits`(대기 횟수), `Innodb_row_lock_time`(누적 ms),
  `Innodb_row_lock_time_avg`, `Innodb_row_lock_current_waits`(순간 대기 수) — before/after diff.
- **라이브 락**: 부하 중 `performance_schema.data_lock_waits` / `SHOW ENGINE INNODB STATUS`의
  TRANSACTIONS 섹션에 같은 행(`id=HOT`)을 기다리는 트랜잭션이 줄 선다.
- **정합**: `like_count` == `COUNT(*) feed_like(completion=HOT)` — 멤버십 게이팅이 맞으면 일치.

## 주의 — 먼저 터질 수 있는 다른 병목

행 락 경합을 보려는데 다른 게 먼저 막히면 그림이 흐려진다:
- **HikariCP 풀**(기본 10): 카운터 UPDATE가 행 락을 기다리는 동안 커넥션을 쥐고 있어 풀이 먼저
  고갈될 수 있다 → 이것도 경합의 증상이지만, 순수 행 락을 보고 싶으면 풀을 키워 분리한다
  (`spring.datasource.hikari.maximum-pool-size`).
- **톰캣 스레드**(기본 200), **max_connections**(MySQL 기본 151): 1000 *TPS* ≠ 1000 동시 커넥션.
  경합은 동시성 수십~수백이면 이미 또렷하다.
