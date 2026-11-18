-- =============================================================
-- 통계 쿼리의 "서비스 장애 전파"를 직접 관측하는 실험
--
-- 가설: 무거운 통계 쿼리(풀스캔)가 도는 동안, 같은 DB를 쓰는
--   고객 트래픽(완료 여부 조회 SELECT + 완료 제출 INSERT)의
--   레이턴시가 치솟는다 — RDS CloudWatch 없이 로컬에서 재현.
--
-- 원리: OLTP 시뮬레이터(프로시저)가 매 쿼리의 소요시간을
--   로그 테이블에 기록 → 통계 쿼리 실행 구간의 초당 레이턴시가
--   전/후 구간과 몇 배 차이 나는지 시계열로 비교한다.
--
-- 사용법 (Workbench 탭 2개):
--   [탭 A] STEP 1~2 실행 → CALL oltp_load(90);  (90초간 블로킹)
--   [탭 B] 시작 ~30초 후 STEP 3의 통계 쿼리 실행 (2~3회 연속이면 더 선명)
--   [탭 A] CALL 끝나면 STEP 4 분석 쿼리 실행
--
-- 증폭 조건:
--   - 통계 쿼리에 IGNORE INDEX로 PRIMARY 풀스캔(813MB) 강제
--     → "통계 인덱스/테이블이 없던 시절" 재현
--   - 버퍼풀 128MB << 테이블 813MB → 디스크 I/O 경합이 실제로 발생
-- =============================================================

USE peaktime_db;

-- ---------- STEP 1. 레이턴시 로그 테이블 ----------
DROP TABLE IF EXISTS oltp_latency_log;
CREATE TABLE oltp_latency_log (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts         DATETIME(6)   NOT NULL,
    op         VARCHAR(10)   NOT NULL,   -- 'select' | 'insert'
    elapsed_ms DECIMAL(10,3) NOT NULL
);

-- ---------- STEP 2. OLTP 시뮬레이터 프로시저 ----------
DROP PROCEDURE IF EXISTS oltp_load;

DELIMITER //
CREATE PROCEDURE oltp_load(IN duration_sec INT)
BEGIN
    DECLARE t_end DATETIME(6) DEFAULT TIMESTAMPADD(SECOND, duration_sec, SYSDATE(6));
    DECLARE t0  DATETIME(6);
    DECLARE uid BIGINT;
    DECLARE mid BIGINT;
    DECLARE dummy INT;

    -- 랜덤 user_id는 FK(users)에 없음 — 시드와 동일하게 세션에서 해제
    SET SESSION foreign_key_checks = 0;

    WHILE SYSDATE(6) < t_end DO
        SET uid = FLOOR(1 + RAND() * 500000);
        SET mid = FLOOR(1 + RAND() * 100);

        -- 고객 쿼리 1: "오늘 이 미션 완료했나?" (uk 인덱스 point read)
        SET t0 = SYSDATE(6);
        SELECT COUNT(*) INTO dummy FROM user_mission_completion
        WHERE user_id = uid AND mission_id = mid AND completed_date = CURDATE();
        INSERT INTO oltp_latency_log (ts, op, elapsed_ms)
        VALUES (t0, 'select', TIMESTAMPDIFF(MICROSECOND, t0, SYSDATE(6)) / 1000);

        -- 고객 쿼리 2: "미션 완료 제출" (uk 충돌은 IGNORE — 시드와 동일 처리)
        SET t0 = SYSDATE(6);
        INSERT IGNORE INTO user_mission_completion
            (created_at, completed_date, mission_type, object_key,
             mission_id, solar_term_id, user_id)
        VALUES
            (SYSDATE(6), CURDATE(), 'DAILY',
             IF(RAND() < 0.3, CONCAT('photo/', uid, '/', mid, '.jpg'), NULL),
             mid, 1, uid);
        INSERT INTO oltp_latency_log (ts, op, elapsed_ms)
        VALUES (t0, 'insert', TIMESTAMPDIFF(MICROSECOND, t0, SYSDATE(6)) / 1000);

        DO SLEEP(0.04);   -- 루프당 ~40ms → 초당 약 20회 × 2쿼리 = 40 ops/s
    END WHILE;
END//
DELIMITER ;

-- 실행 (90초 동안 이 탭은 블로킹됨):
-- CALL oltp_load(90);

-- ---------- STEP 3. [다른 탭에서] 무거운 통계 쿼리 ----------
-- CALL 시작 ~30초 후 실행. IGNORE INDEX로 813MB PRIMARY 풀스캔 강제.
-- 한 번에 ~5초 — 2~3회 연속 실행하면 영향 구간이 더 선명해진다.
/*
SELECT completed_date, COUNT(*) AS completion_count, COUNT(object_key) AS photo_count
FROM user_mission_completion IGNORE INDEX (idx_completion_completed_date)
WHERE completed_date BETWEEN DATE_SUB(CURDATE(), INTERVAL 364 DAY) AND CURDATE()
GROUP BY completed_date
ORDER BY completed_date;
*/

-- ---------- STEP 4. 결과 분석: 초당 레이턴시 시계열 ----------
-- 통계 쿼리 실행 구간(STEP 3을 날린 시각)의 avg/max가
-- 전/후 구간 대비 몇 배 튀는지 확인한다.
/*
SELECT DATE_FORMAT(ts, '%H:%i:%s') AS sec,
       COUNT(*)                                            AS ops,
       ROUND(AVG(IF(op = 'select', elapsed_ms, NULL)), 1)  AS sel_avg_ms,
       MAX(IF(op = 'select', elapsed_ms, NULL))            AS sel_max_ms,
       ROUND(AVG(IF(op = 'insert', elapsed_ms, NULL)), 1)  AS ins_avg_ms,
       MAX(IF(op = 'insert', elapsed_ms, NULL))            AS ins_max_ms
FROM oltp_latency_log
GROUP BY sec
ORDER BY sec;
*/

-- 전체 요약 (통계 쿼리 구간을 수동으로 지정해서 전/중/후 비교):
/*
SELECT op, COUNT(*) AS cnt,
       ROUND(AVG(elapsed_ms), 2) AS avg_ms,
       MAX(elapsed_ms)           AS max_ms
FROM oltp_latency_log
-- WHERE ts BETWEEN '2026-10-26 20:00:30' AND '2026-10-26 20:00:45'  -- 통계 구간
GROUP BY op;
*/

-- ---------- 정리 (실험 반복 시) ----------
-- TRUNCATE TABLE oltp_latency_log;        -- 로그만 초기화
-- DROP PROCEDURE IF EXISTS oltp_load;     -- 완전 제거
-- DROP TABLE IF EXISTS oltp_latency_log;