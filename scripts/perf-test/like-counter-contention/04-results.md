# 결과 — 좋아요 카운터 행 락 경합

> 채워 넣는 골격. 실행 환경·수치는 측정 후 기록.

## 환경

| 항목 | 값 |
|------|-----|
| MySQL | (버전 / 로컬 or RDS 인스턴스) |
| 버퍼 풀 | (innodb_buffer_pool_size) |
| HikariCP max pool | (기본 10 / 조정값) |
| 앱 프로필 | prod,loadtest |
| 핫 completion id | 8000001 |

## A. 단일 행 카운터 (baseline)

현재 설계: `UPDATE user_mission_completion SET like_count = like_count + 1 WHERE id = ?`

### 부하 조건별 측정

| RPS(목표) | 동시 VU | 실측 RPS | p95 (ms) | p99 (ms) | dropped_iter | Δ row_lock_waits | Δ row_lock_time(ms) | row_lock_time_avg |
|-----------|---------|----------|----------|----------|--------------|------------------|---------------------|-------------------|
| 500       |         |          |          |          |              |                  |                     |                   |
| 1000      |         |          |          |          |              |                  |                     |                   |
| 1500      |         |          |          |          |              |                  |                     |                   |

### 관찰
- (실측 RPS가 목표를 못 따라가기 시작하는 지점 = 경합 천장)
- (p99가 비선형으로 튀는 지점)
- (SHOW ENGINE INNODB STATUS에서 id=8000001 행을 기다리는 트랜잭션 줄서기 캡처)

### 정합
- `like_count_col` == `feed_like_rows` ? (멤버십 게이팅 검증)

---

## B. 슬롯 카운터 (A/B 비교 — 추후)

설계: `feed_like_slot(completion_id, slot, count)` + `UNIQUE(completion_id, slot)`,
`INSERT ... ON DUPLICATE KEY UPDATE count = count + 1` 랜덤 슬롯, 읽기는 `SUM(count)`.

| RPS(목표) | 슬롯 수 | 실측 RPS | p95 (ms) | p99 (ms) | dropped_iter | Δ row_lock_waits | Δ row_lock_time(ms) |
|-----------|---------|----------|----------|----------|--------------|------------------|---------------------|
| 1000      | 16      |          |          |          |              |                  |                     |
| 1000      | 32      |          |          |          |              |                  |                     |

### 결론 (단일 행 대비)
- (row_lock_waits 감소폭)
- (동일 RPS에서 p99 개선폭)
- (슬롯 수 ↑ → 경합 ↓ vs SUM 집계 비용·슬롯 행 수 ↑ 트레이드오프)