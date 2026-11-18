-- =============================================================
-- 좋아요 카운터 경합 관측 쿼리 (MySQL 8)
-- 대상: peaktime_db
--
-- 흐름:
--   STEP 1 — 부하 "직전" 상태 카운터 스냅샷 (다른 세션에서 미리)
--   (k6 부하 실행)
--   STEP 2 — 부하 "중/직후" 상태 카운터 스냅샷 → STEP 1과 diff
--   STEP 3 — 부하 "중" 라이브 락 대기 관측 (반복 실행)
--   STEP 4 — 카운터 정합 검증
-- =============================================================

USE peaktime_db;
SET @HOT := 8000001;

-- ---------- STEP 1. before: 행 락 상태 카운터 ----------
-- 아래 4개 값을 적어둔다(before).
SHOW GLOBAL STATUS WHERE Variable_name IN (
    'Innodb_row_lock_waits',          -- 행 락을 기다려야 했던 누적 횟수
    'Innodb_row_lock_time',           -- 행 락 대기 누적 시간(ms)
    'Innodb_row_lock_time_avg',       -- 평균 대기 시간(ms)
    'Innodb_row_lock_current_waits'   -- 지금 이 순간 대기 중인 수
);

-- ---------- STEP 2. after: 같은 카운터 → diff ----------
-- 부하 끝난 뒤 같은 쿼리 재실행, before와의 차이가 경합의 크기다.
-- (Innodb_row_lock_waits 증가분 ≈ 직렬화로 인해 "줄 서야 했던" UPDATE 수)
SHOW GLOBAL STATUS WHERE Variable_name IN (
    'Innodb_row_lock_waits',
    'Innodb_row_lock_time',
    'Innodb_row_lock_time_avg',
    'Innodb_row_lock_current_waits'
);

-- ---------- STEP 3. [부하 중] 라이브 락 대기 ----------
-- 같은 행(id=@HOT)을 기다리는 트랜잭션들이 보인다. 부하 중 여러 번 실행.
SELECT
    w.requesting_engine_transaction_id AS waiting_trx,
    w.blocking_engine_transaction_id   AS blocking_trx,
    l.object_name,
    l.lock_type,
    l.lock_mode,
    l.lock_status,
    l.lock_data
FROM performance_schema.data_lock_waits w
JOIN performance_schema.data_locks l
  ON l.engine_lock_id = w.requesting_engine_lock_id;

-- 순간 대기 수만 빠르게 보고 싶으면:
-- SHOW GLOBAL STATUS LIKE 'Innodb_row_lock_current_waits';

-- 엔진 레벨 전체 그림(TRANSACTIONS 섹션에서 'lock_wait' 상태 트랜잭션 확인):
-- SHOW ENGINE INNODB STATUS\G

-- ---------- STEP 4. 정합 검증 ----------
-- 멤버십 게이팅이 맞으면 비정규화 카운터 == 실제 좋아요 행 수.
SELECT
    (SELECT like_count FROM user_mission_completion WHERE id = @HOT) AS like_count_col,
    (SELECT COUNT(*)   FROM feed_like WHERE completion_id = @HOT)    AS feed_like_rows;