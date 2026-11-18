-- =============================================================
-- 일별 미션 수행 통계 실험용 user_mission_completion 적재 (MySQL 8 전용)
-- 대상: peaktime_db (application-local.yml의 앱 로컬 DB)
--
-- 전제: 앱을 한 번 기동해서 Hibernate(ddl-auto: update)가 스키마를
--       만들어둔 상태여야 한다. 테이블 DDL은 이 스크립트가 아니라
--       엔티티가 주인이므로 여기서 DROP/CREATE 하지 않는다.
--
-- 운영 스키마 그대로라서 perf_test 때와 달리 두 가지 제약을 우회한다:
--   · FK(user/mission/solar_term): 랜덤 ID가 실존하지 않으므로 세션에서 체크 해제
--   · 유니크(user_id, mission_id, completed_date): 랜덤 조합이 드물게 충돌
--     (1,000만 건 기준 ≈ 3천 건, 0.03%) → INSERT IGNORE로 건너뜀
--
-- 사용법:
--   mysql -uroot -p peaktime_db < 01-seed.sql
--   1,000만 건 기준 약 20~40분. 진행 상황은 다른 세션에서
--   SELECT COUNT(*) FROM peaktime_db.user_mission_completion; (1만 건마다 커밋)
--
-- ※ 기존 perf_test에 이미 적재해뒀다면 프로시저 대신 복사가 훨씬 빠르다 (수 분):
--   SET FOREIGN_KEY_CHECKS = 0;
--   INSERT IGNORE INTO peaktime_db.user_mission_completion
--       (user_id, mission_id, solar_term_id, mission_type,
--        object_key, memo, completed_date, created_at, updated_at)
--   SELECT user_id, mission_id, solar_term_id, mission_type,
--          object_key, memo, completed_date, created_at, updated_at
--   FROM perf_test.user_mission_completion;
--   SET FOREIGN_KEY_CHECKS = 1;
--   ANALYZE TABLE peaktime_db.user_mission_completion;
-- =============================================================

USE peaktime_db;

-- 재적재 시 초기화 (참조당하는 테이블이 아니라 TRUNCATE 가능)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_mission_completion;

-- 데이터 분포 설계:
--   user_id        : 1 ~ 500,000 균등
--   mission_id     : 1 ~ 100 균등
--   completed_date : 오늘 포함 과거 365일 균등 → 하루 ≈ 27,400건,
--                    한 달 범위 조건 ≈ 8.5% 선택도.
--                    ★ 미래 날짜를 만들지 않는다 — 운영 불변식(completed_date ≤ 오늘)을
--                    지켜야 하이브리드 조회의 미확정 분기가 "오늘 하루"로 유지된다.
--                    미래분이 있으면 그 전체가 미확정 분기로 쏟아져 실험이 왜곡됨
--   solar_term_id  : completed_date에서 유도 (약 15일 = 1절기, 1~24)
--   mission_type   : DAILY 50% / RECOMMENDED 30% / SELECTED 20%
--   object_key     : 30% 확률로 존재 (사진 인증 완료) → photoCount 집계 재료.
--                    NULL 70%는 COUNT(object_key)의 null 제외 집계를 검증
--   created_at     : completed_date 당일 랜덤 시각
DELIMITER $$

DROP PROCEDURE IF EXISTS seed_user_mission_completion $$
CREATE PROCEDURE seed_user_mission_completion(IN p_total BIGINT)
BEGIN
    DECLARE i BIGINT DEFAULT 0;
    DECLARE v_cd DATE;
    DECLARE v_ca DATETIME(6);
    DECLARE v_type_roll DOUBLE;

    -- 프로시저 안에서도 FK 체크 해제 상태 보장 (세션 변수)
    SET FOREIGN_KEY_CHECKS = 0;

    -- 건별 커밋이면 fsync 1,000만 번이라 하루 종일 걸림 → 1만 건 단위 커밋
    SET autocommit = 0;

    WHILE i < p_total DO
        SET v_cd = DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 365) DAY);
        SET v_ca = TIMESTAMP(v_cd) + INTERVAL FLOOR(RAND() * 86400) SECOND;
        SET v_type_roll = RAND();

        -- IGNORE: 유니크(user, mission, date) 충돌은 건너뜀 (총량 대비 0.03% 수준)
        INSERT IGNORE INTO user_mission_completion
            (user_id, mission_id, solar_term_id, mission_type,
             object_key, memo, completed_date, created_at, updated_at)
        VALUES (
            1 + FLOOR(RAND() * 500000),
            1 + FLOOR(RAND() * 100),
            LEAST(24, 1 + FLOOR((DAYOFYEAR(v_cd) - 1) / 15.3)),
            IF(v_type_roll < 0.5, 'DAILY', IF(v_type_roll < 0.8, 'RECOMMENDED', 'SELECTED')),
            IF(RAND() < 0.3, CONCAT('records/seed-', i, '.jpg'), NULL),
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

CALL seed_user_mission_completion(10000000);   -- ← 적재 건수 조절

SET FOREIGN_KEY_CHECKS = 1;

-- 통계 갱신 (옵티마이저가 실제 분포를 보고 판단하도록)
ANALYZE TABLE user_mission_completion;

SELECT COUNT(*)              AS total_rows,
       MIN(completed_date)   AS min_date,
       MAX(completed_date)   AS max_date,
       COUNT(object_key)     AS photo_rows
FROM user_mission_completion;
