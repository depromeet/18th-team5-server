-- =============================================================
-- 조인 + 커버링 인덱스 실험용 "편중 + 산포" 데이터 적재 (MySQL 8 전용)
--
-- 사용법:
--   mysql -uroot -p < 01-seed.sql
--   또는 docker: docker exec -i perf-mysql mysql -uroot -proot < 01-seed.sql
--   users 250만 + 로그 420만 건, 15~30분 소요.
--
-- ★ 설계 축 1 — 편중(skew): 옵티마이저가 "잘못" 추정하게 만든다
--   NATURE_OUTDOOR는 전체의 2%(5만 명)인데 로그가 평균 10건,
--   나머지 98%는 평균 1.5건 → 전체 평균(rec_per_key) ≈ 2.
--   조인 드리븐 추정은 전체 평균만 쓰므로 EXPLAIN rows≈2 vs 실측 10.
--   (≒ VIP 2%가 주문 평균 10건, 전체 평균 1.8건인 member/orders 상황)
--
-- ★ 설계 축 2 — 산포(scatter): 랜덤 I/O를 "진짜" 랜덤으로 만든다
--   로그를 유저 순서로 몰아 넣으면 한 유저의 10건이 같은 페이지에
--   연속으로 박혀서, 룩업 10번이 사실상 페이지 1개 순차 읽기가 된다
--   (이전 버전의 실패 — 테이블을 키워도 룩업 단가가 안 오른 이유).
--   실제 서비스처럼 여러 유저의 로그가 시간순으로 뒤섞여 쌓이도록
--   "한 건마다 무작위 유저 배정"으로 적재 → 한 유저의 10건이
--   서로 다른 페이지에 흩어져 룩업마다 진짜 랜덤 액세스가 난다.
--   랜덤 I/O는 행 수(논리)가 아니라 페이지 산포(물리)가 만든다.
--
--   NATURE 유저를 무작위로 뽑을 수 있도록 유저 타입을 id로 결정:
--   id % 50 = 1 인 유저(1, 51, 101, ...)가 NATURE_OUTDOOR — 정확히 2%.
--
-- ※ 주의: users / user_mission_log를 DROP 후 재적재한다.
--   500만 건 단일 테이블 실험으로 복귀하려면 user-mission-log/01-seed.sql 재실행.
-- =============================================================

USE perf_test;

-- -------------------------------------------------------------
-- 1. users (조인 드라이빙 테이블, 250만 건)
--   루프 수 = NATURE 유저 수 = 250만 × 2% = 5만
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
    DECLARE i BIGINT DEFAULT 0;    -- id = i + 1
    DECLARE v_ca DATETIME(6);

    SET autocommit = 0;

    WHILE i < p_total DO
        SET v_ca = TIMESTAMP('2026-01-01') + INTERVAL FLOOR(RAND() * 86400 * 365) SECOND;

        INSERT INTO users (nickname, user_type, created_at, updated_at)
        VALUES (
            CONCAT('user-', i),
            -- id % 50 = 1 → NATURE_OUTDOOR (정확히 2%), 나머지는 4타입 무작위
            IF((i + 1) % 50 = 1,
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

CALL seed_users(2500000);
ANALYZE TABLE users;

-- -------------------------------------------------------------
-- 2. user_mission_log (420만 건) — 한 건마다 무작위 유저 배정 (산포 확보)
--   12% 확률 → 무작위 NATURE 유저 (50만 건 ÷ 5만 명 = 평균 10건)
--   88% 확률 → 무작위 일반 유저   (370만 건 ÷ 245만 명 = 평균 1.5건)
--   삽입 순서가 user_id와 무관하므로 물리 저장(PK 순)이 유저 기준으로 흩어진다
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
CREATE PROCEDURE seed_user_mission_log(IN p_total BIGINT)
BEGIN
    DECLARE i BIGINT DEFAULT 0;
    DECLARE v_uid BIGINT;
    DECLARE v_cd DATE;
    DECLARE v_ca DATETIME(6);

    SET autocommit = 0;

    WHILE i < p_total DO
        IF RAND() < 0.12 THEN
            -- NATURE 유저 (id = 1, 51, 101, ... 2,499,951) 중 무작위
            SET v_uid = 1 + 50 * FLOOR(RAND() * 50000);
        ELSE
            -- 일반 유저 중 무작위 (NATURE id가 뽑히면 한 칸 비켜감)
            SET v_uid = 1 + FLOOR(RAND() * 2500000);
            IF v_uid % 50 = 1 THEN
                SET v_uid = v_uid + 1;
            END IF;
        END IF;

        SET v_cd = DATE_ADD('2026-01-01', INTERVAL FLOOR(RAND() * 365) DAY);
        SET v_ca = TIMESTAMP(v_cd) + INTERVAL FLOOR(RAND() * 86400) SECOND;

        INSERT INTO user_mission_log
            (idempotency_key, user_id, solar_term_id, completed_date, created_at, updated_at)
        VALUES (
            CONCAT('seed-', i),
            v_uid,
            1 + FLOOR(RAND() * 24),
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

CALL seed_user_mission_log(4200000);
ANALYZE TABLE user_mission_log;

-- -------------------------------------------------------------
-- 3. 검증 — 이 숫자들이 실험의 전제
-- -------------------------------------------------------------
-- 전체 평균 (rec_per_key가 보는 값): ≈ 2 기대
SELECT COUNT(*) / COUNT(DISTINCT user_id) AS global_avg_logs,
       COUNT(*) AS total_rows
FROM user_mission_log;

-- 타입별 실제 평균: NATURE_OUTDOOR만 ≈ 10, 유저 수 5만 확인
SELECT u.user_type,
       COUNT(DISTINCT u.id) AS users,
       COUNT(l.id) / COUNT(DISTINCT u.id) AS avg_logs_per_user
FROM users u
LEFT JOIN user_mission_log l ON l.user_id = u.id
GROUP BY u.user_type;

-- 산포 확인: 한 NATURE 유저의 로그 PK(id)가 넓게 흩어져 있어야 한다
-- (유저 순회 적재였다면 id가 연속 — min~max 폭이 건수와 비슷하게 좁음)
SELECT user_id, COUNT(*) AS cnt, MIN(id) AS min_pk, MAX(id) AS max_pk
FROM user_mission_log
WHERE user_id = 51
GROUP BY user_id;


-- =============================================================
-- 실험 대상 쿼리 (같은 유저 타입 미션 랭킹 TOP 20)
--
--   member/orders 예시와 1:1 대응:
--     u.user_type = 'NATURE_OUTDOOR' (2%)  ≒ m.grade = 'VIP' (2%)
--     COUNT(l.id)                          ≒ COUNT(o.order_id)
--     MAX(l.created_at)                    ≒ SUM(o.total_price)
--
--   관찰 포인트: EXPLAIN은 드리븐 rows≈2 (전체 평균 통계),
--   EXPLAIN ANALYZE 실측은 rows≈10 loops≈50000 — 추정의 5배.
--   이번엔 로그가 물리적으로 흩어져 있어 룩업 50만 회가 진짜 랜덤 액세스.
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