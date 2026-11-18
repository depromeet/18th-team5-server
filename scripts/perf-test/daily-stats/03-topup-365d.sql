-- =============================================================
-- (선택) 과거 빈 구간 채우기 — 365일 꽉 채우고 싶을 때만 (MySQL 8 전용)
-- 대상: peaktime_db
--
-- 배경: 최초 시드가 2026-01-01부터였고 미래분(오늘 이후)을 삭제해서,
--   현재 데이터는 2026-01-01 ~ 오늘(297일). 과거 쪽 2025-10-25 ~ 2025-12-31
--   (68일)이 비어 있다. 이 구간만 하루 ≈ 27,400건 밀도로 채운다 (약 186만 건, ~5분).
--
-- 재시드(01-seed, 20~40분)와 달리 기존 데이터는 건드리지 않는다.
-- 실험 결론("행 수 비례 vs 날짜 수 비례")에는 297일로도 충분하므로 필수 아님.
--
-- 사용법:
--   mysql -uroot -p peaktime_db < 03-topup-365d.sql
--   이후 02-experiment.sql을 돌리면 [3]의 백필이 새 구간까지 포함한다
-- =============================================================

USE peaktime_db;

SET FOREIGN_KEY_CHECKS = 0;

DELIMITER $$

DROP PROCEDURE IF EXISTS topup_missing_window $$
CREATE PROCEDURE topup_missing_window(IN p_total BIGINT)
BEGIN
    DECLARE i BIGINT DEFAULT 0;
    DECLARE v_cd DATE;
    DECLARE v_ca DATETIME(6);
    DECLARE v_type_roll DOUBLE;

    SET FOREIGN_KEY_CHECKS = 0;
    SET autocommit = 0;

    WHILE i < p_total DO
        -- 빈 구간(2025-10-25 ~ 2025-12-31)만 균등 분포
        SET v_cd = DATE_SUB('2025-12-31', INTERVAL FLOOR(RAND() * 68) DAY);
        SET v_ca = TIMESTAMP(v_cd) + INTERVAL FLOOR(RAND() * 86400) SECOND;
        SET v_type_roll = RAND();

        INSERT IGNORE INTO user_mission_completion
            (user_id, mission_id, solar_term_id, mission_type,
             object_key, memo, completed_date, created_at, updated_at)
        VALUES (
            1 + FLOOR(RAND() * 500000),
            1 + FLOOR(RAND() * 100),
            LEAST(24, 1 + FLOOR((DAYOFYEAR(v_cd) - 1) / 15.3)),
            IF(v_type_roll < 0.5, 'DAILY', IF(v_type_roll < 0.8, 'RECOMMENDED', 'SELECTED')),
            IF(RAND() < 0.3, CONCAT('records/topup-', i, '.jpg'), NULL),
            NULL,
            v_cd,
            v_ca,
            v_ca
        );

        SET i = i + 1;
        IF i % 10000 = 0 THEN
            COMMIT;
        END IF;
    END WHILE;

    COMMIT;
    SET autocommit = 1;
END $$

DELIMITER ;

CALL topup_missing_window(1860000);   -- 68일 × 27,400건

SET FOREIGN_KEY_CHECKS = 1;

ANALYZE TABLE user_mission_completion;

SELECT COUNT(*)            AS total_rows,
       MIN(completed_date) AS min_date,
       MAX(completed_date) AS max_date
FROM user_mission_completion;