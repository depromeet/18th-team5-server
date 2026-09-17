-- =============================================================
-- 조인 + 커버링 인덱스 실험용 "편중 분포" 데이터 적재 (MySQL 8 전용)
--
-- 사용법:
--   mysql -uroot -p < 01-seed.sql
--   또는 docker: docker exec -i perf-mysql mysql -uroot -proot < 01-seed.sql
--   users 100만 + 로그 약 170만 건, 5~10분 소요.
--
-- ★ 핵심 설계 — 옵티마이저가 "잘못" 판단하게 만드는 편중:
--   member/orders 예시의 실제 원인 재현 — VIP는 전체 회원의 2%인데
--   주문이 평균 10건, 전체 평균은 1.8건. rec_per_key(인덱스 통계)는
--   전체 평균만 알아서 EXPLAIN은 rows≈2로 추정하지만 실측은 10.
--   → 옵티마이저는 NLJ를 싸게 보고 선택하지만, 실제 랜덤 I/O는
--     추정의 5배(2만 루프 × 10행 = 20만 회)로 터진다.
--
--   NATURE_OUTDOOR : 전체의 2% (2만 명) — 로그 8~12건 (평균 10)  ≒ VIP
--   나머지 4타입   : 98% — 로그 0~3건 (평균 1.5)
--   → 전체 rec_per_key ≈ 2, NATURE 세그먼트 실제 팬아웃 = 10
--
-- ※ 주의: users / user_mission_log를 DROP 후 재적재한다.
--   500만 건 단일 테이블 실험으로 복귀하려면 user-mission-log/01-seed.sql 재실행.
-- =============================================================

USE perf_test;

-- -------------------------------------------------------------
-- 1. users (조인 드라이빙 테이블, 100만 건)
-- -------------------------------------------------------------
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL,
    user_type VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

DELIMITER $$

DROP PROCEDURE IF EXISTS seed_users $$
CREATE PROCEDURE seed_users(IN p_total BIGINT)
BEGIN
    DECLARE i BIGINT DEFAULT 0;
    DECLARE v_ca DATETIME(6);

    SET autocommit = 0;

    WHILE i < p_total DO
        SET v_ca = TIMESTAMP('2026-01-01') + INTERVAL FLOOR(RAND() * 86400 * 365) SECOND;

        INSERT INTO users (nickname, user_type, created_at, updated_at)
        VALUES (
            CONCAT('user-', i),
            -- 2%만 NATURE_OUTDOOR (≒ VIP 2%), 나머지는 4타입 균등
            IF(RAND() < 0.02,
               'NATURE_OUTDOOR',
               ELT(1 + FLOOR(RAND() * 4),
                   'SEASONAL_FOOD', 'EMOTIONAL_CONTENT',
                   'RECORD_LOVER', 'SENSE_SEEKER')),
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

CALL seed_users(1000000);
ANALYZE TABLE users;

-- -------------------------------------------------------------
-- 2. user_mission_log — 유저를 순회하며 타입에 따라 건수를 편중 적재
--   NATURE_OUTDOOR : 8 + RAND()*5 → 8~12건 (평균 10)
--   그 외          : RAND()*4     → 0~3건 (평균 1.5, 일부는 0건)
-- -------------------------------------------------------------
DROP TABLE IF EXISTS user_mission_log;
CREATE TABLE user_mission_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    solar_term_id BIGINT NOT NULL,
    completed_date DATE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- 조인 실험의 0단계 기본 인덱스는 FK 조인용 idx_user_id 하나만
CREATE INDEX idx_user_id ON user_mission_log(user_id);

DELIMITER $$

DROP PROCEDURE IF EXISTS seed_user_mission_log $$
CREATE PROCEDURE seed_user_mission_log()
BEGIN
    DECLARE v_uid BIGINT DEFAULT 0;
    DECLARE v_max BIGINT;
    DECLARE v_type VARCHAR(30);
    DECLARE v_cnt INT;
    DECLARE j INT;
    DECLARE v_cd DATE;
    DECLARE v_ca DATETIME(6);

    SELECT MAX(id) INTO v_max FROM users;

    SET autocommit = 0;

    WHILE v_uid < v_max DO
        SET v_uid = v_uid + 1;

        SELECT user_type INTO v_type FROM users WHERE id = v_uid;
        SET v_cnt = IF(v_type = 'NATURE_OUTDOOR',
                       8 + FLOOR(RAND() * 5),   -- 8~12건, 평균 10
                       FLOOR(RAND() * 4));      -- 0~3건, 평균 1.5

        SET j = 0;
        WHILE j < v_cnt DO
            SET v_cd = DATE_ADD('2026-01-01', INTERVAL FLOOR(RAND() * 365) DAY);
            SET v_ca = TIMESTAMP(v_cd) + INTERVAL FLOOR(RAND() * 86400) SECOND;

            INSERT INTO user_mission_log
                (idempotency_key, user_id, solar_term_id, completed_date, created_at, updated_at)
            VALUES (
                CONCAT('seed-', v_uid, '-', j),
                v_uid,
                1 + FLOOR(RAND() * 24),
                v_cd,
                v_ca,
                v_ca
            );
            SET j = j + 1;
        END WHILE;

        IF v_uid % 10000 = 0 THEN
            COMMIT;
        END IF;
    END WHILE;

    COMMIT;
    SET autocommit = 1;
END $$

DELIMITER ;

CALL seed_user_mission_log();
ANALYZE TABLE user_mission_log;

-- -------------------------------------------------------------
-- 3. 편중 검증 — 이 숫자들이 실험의 전제
-- -------------------------------------------------------------
-- 전체 평균 (rec_per_key가 보는 값): ≈ 2 기대
SELECT COUNT(*) / COUNT(DISTINCT user_id) AS global_avg_logs,
       COUNT(*) AS total_rows
FROM user_mission_log;

-- 타입별 실제 평균: NATURE_OUTDOOR만 ≈ 10 기대
SELECT u.user_type,
       COUNT(u.id) AS users,
       COUNT(l.id) / COUNT(DISTINCT u.id) AS avg_logs_per_user
FROM users u
LEFT JOIN user_mission_log l ON l.user_id = u.id
GROUP BY u.user_type;


-- =============================================================
-- 실험 대상 쿼리 (같은 유저 타입 미션 랭킹 TOP 20)
--
--   member/orders 예시와 1:1 대응:
--     u.user_type = 'NATURE_OUTDOOR' (2%)  ≒ m.grade = 'VIP' (2%)
--     COUNT(l.id)                          ≒ COUNT(o.order_id)
--     MAX(l.created_at)                    ≒ SUM(o.total_price)
--
--   관찰 포인트: EXPLAIN은 드리븐 rows≈2 (전체 평균 통계),
--   EXPLAIN ANALYZE 실측은 rows≈10 loops≈20000 — 추정의 5배.
--   옵티마이저가 싸다고 믿고 고른 NLJ에서 랜덤 I/O 20만 회가 터진다.
-- =============================================================
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- =============================================================
-- [옵션] 정리
-- =============================================================
-- DROP TABLE IF EXISTS users;
-- 500만 건 단일 테이블 실험으로 복귀: user-mission-log/01-seed.sql 재실행