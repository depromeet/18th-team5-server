-- =============================================================
-- 일별 통계 3단 비교 실험 (MySQL 8 전용) — 01-seed.sql 적재 후 실행
-- 대상: peaktime_db (application-local.yml의 앱 로컬 DB)
--
--   [1] 베이스라인: completed_date 인덱스 없이 1년 집계
--   [2] 인덱스 추가 후 동일 쿼리
--       → "인덱스를 타도 범위가 크면 느리다"를 수치로 확정
--       → 단, 하루치 범위(하이브리드의 실시간 파트)는 인덱스로 즉답임을 함께 확인
--   [3] 통계 테이블 백필 + UNION ALL 하이브리드 (서비스 쿼리와 동일 형태)
--       → 읽기량이 행 수(1,000만)가 아닌 날짜 수(365)에 비례함을 확인
--
-- 전제: 앱이 한 번 기동해 Hibernate가 스키마를 만들어둔 상태.
--   엔티티에 idx_completion_completed_date가 선언돼 있어 인덱스가 이미
--   존재할 수 있다 → [1]에서 조건부로 떼고 측정, [2]에서 되살린다
--   (ddl-auto: update라 다음 앱 기동 때도 자동 복원됨).
--
-- 사용법:
--   mysql -uroot -p peaktime_db < 02-experiment.sql
--   (버퍼풀 영향을 보려면 각 측정 쿼리를 두 번씩 실행해 cold/warm 구분)
-- =============================================================

USE peaktime_db;

-- -------------------------------------------------------------
-- [1] 베이스라인 — completed_date 인덱스 없이 1년 범위 풀스캔 + GROUP BY
-- -------------------------------------------------------------
-- MySQL은 DROP INDEX IF EXISTS가 없어 information_schema로 조건부 처리
DELIMITER $$
DROP PROCEDURE IF EXISTS drop_completed_date_idx $$
CREATE PROCEDURE drop_completed_date_idx()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = 'peaktime_db'
                 AND table_name = 'user_mission_completion'
                 AND index_name = 'idx_completion_completed_date') THEN
        ALTER TABLE user_mission_completion DROP INDEX idx_completion_completed_date;
    END IF;
END $$
DELIMITER ;
CALL drop_completed_date_idx();

EXPLAIN ANALYZE
SELECT completed_date, COUNT(*) AS completion_count, COUNT(object_key) AS photo_count
FROM user_mission_completion
WHERE completed_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 364 DAY) AND CURDATE()
GROUP BY completed_date
ORDER BY completed_date;

-- -------------------------------------------------------------
-- [2] completed_date 인덱스 생성 (엔티티 선언과 동일 이름) — 1,000만 건이라 수 분 소요
-- -------------------------------------------------------------
CREATE INDEX idx_completion_completed_date ON user_mission_completion (completed_date);

-- [2-a] 같은 1년 쿼리 — 범위가 전체의 100%라 옵티마이저가 인덱스를 버리고
--       풀스캔을 택하거나, 인덱스를 타도 행 룩업 때문에 더 느릴 수 있다.
--       "인덱스는 큰 범위를 구제하지 못한다"의 증거
EXPLAIN ANALYZE
SELECT completed_date, COUNT(*) AS completion_count, COUNT(object_key) AS photo_count
FROM user_mission_completion
WHERE completed_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 364 DAY) AND CURDATE()
GROUP BY completed_date
ORDER BY completed_date;

-- [2-b] 하루치(하이브리드의 실시간 파트가 실제로 읽는 양) — 인덱스 range scan ≈ 27,000행.
--       통계 테이블 전환 후 원본이 감당할 몫은 이것뿐이다
EXPLAIN ANALYZE
SELECT completed_date, COUNT(*) AS completion_count, COUNT(object_key) AS photo_count
FROM user_mission_completion
WHERE completed_date = CURDATE()
GROUP BY completed_date;

-- -------------------------------------------------------------
-- [3] 통계 테이블 백필 + 하이브리드 조회
-- -------------------------------------------------------------
-- 테이블은 Hibernate(DailyCompletionStats 엔티티)가 이미 만들어둠 — 내용만 초기화.
-- 백필은 확정 배치(DailyCompletionStatsBatchService)가 하는 일을 수동으로 재현한 것
TRUNCATE TABLE daily_completion_stats;

INSERT INTO daily_completion_stats (stat_date, completion_count, photo_count, created_at, updated_at)
SELECT completed_date, COUNT(*), COUNT(object_key), NOW(6), NOW(6)
FROM user_mission_completion
WHERE completed_date < CURDATE()
GROUP BY completed_date;

-- [3-a] 서비스 쿼리(DailyCompletionStatsRepository.findDailyStats)와 동일한 UNION ALL — 1년 범위.
--       위쪽: 통계 테이블 ~365행 / 아래쪽: 경계(어제) 이후만 원본 인덱스 range scan
EXPLAIN ANALYZE
SELECT s.stat_date AS statDate,
       s.completion_count AS completionCount,
       s.photo_count AS photoCount
FROM daily_completion_stats s
WHERE s.stat_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 364 DAY) AND CURDATE()

UNION ALL

SELECT c.completed_date, COUNT(*), COUNT(c.object_key)
FROM user_mission_completion c
WHERE c.completed_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 364 DAY) AND CURDATE()
  AND c.completed_date > (SELECT COALESCE(MAX(stat_date), CAST('1000-01-01' AS DATE))
                          FROM daily_completion_stats)
GROUP BY c.completed_date
ORDER BY statDate;

-- [3-b] 범위가 전부 확정분인 경우 — 아래 분기가 빈 인덱스 범위로 즉시 끝나는지 확인
EXPLAIN ANALYZE
SELECT s.stat_date AS statDate,
       s.completion_count AS completionCount,
       s.photo_count AS photoCount
FROM daily_completion_stats s
WHERE s.stat_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 364 DAY)
                      AND DATE_SUB(CURDATE(), INTERVAL 180 DAY)

UNION ALL

SELECT c.completed_date, COUNT(*), COUNT(c.object_key)
FROM user_mission_completion c
WHERE c.completed_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 364 DAY)
                           AND DATE_SUB(CURDATE(), INTERVAL 180 DAY)
  AND c.completed_date > (SELECT COALESCE(MAX(stat_date), CAST('1000-01-01' AS DATE))
                          FROM daily_completion_stats)
GROUP BY c.completed_date

ORDER BY statDate;

-- -------------------------------------------------------------
-- 측정 후: 앱 API로도 같은 결과가 나오는지 확인 (날짜는 오늘 기준으로 조정)
--   GET http://localhost:8080/api/v1/stats/daily-completions?startDate=2025-10-25&endDate=2026-10-24
-- -------------------------------------------------------------
