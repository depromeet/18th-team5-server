-- =============================================================
-- 조인 + 커버링 인덱스 실험용 데이터 적재 (MySQL 8 전용)
--
-- 사용법:
--   mysql -uroot -p < 01-seed.sql
--   또는 docker: docker exec -i perf-mysql mysql -uroot -proot < 01-seed.sql
--
-- ★ 데이터 설계가 곧 실험이다: 로그를 "유저당 평균 10건"으로 적재한다.
--   이전 시도(유저당 50건 + 날짜 범위로 10건 축소)는 실패 — 옵티마이저는
--   조인 드리븐 추정에 인덱스 통계(rec_per_key = 유저당 50건)만 쓰고,
--   범위 조건이 10건으로 깎는 건 비용에 반영 못 해서 users 드라이빙을
--   항상 비싸게 보고 조인을 뒤집었다(l 풀스캔 → u PK lookup rows=1).
--   통계 자체가 10이어야 힌트 없이 원하는 NLJ가 나온다.
--   (member/orders 예시가 자연스럽게 됐던 이유 = 회원당 주문이 '원래' 10건)
--
-- ※ 주의: user_mission_log를 TRUNCATE 후 100만 건으로 재적재한다.
--   기존 500만 건 실험(user-mission-log/)으로 돌아가려면 해당 01-seed.sql 재실행.
-- =============================================================

USE perf_test;

-- -------------------------------------------------------------
-- 1. users (조인 드라이빙 테이블, 10만 건)
--   user_type : 5종 균등 → 타입당 2만 명 (선택도 20%, WHERE 풀스캔 방지 축)
--   nickname  : 커버링 실험에서 SELECT 대상이 되는 비인덱스 컬럼
--   이미 적재돼 있으면 이 구간은 건너뛰어도 된다.
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
            ELT(1 + FLOOR(RAND() * 5),
                'NATURE_OUTDOOR', 'SEASONAL_FOOD', 'EMOTIONAL_CONTENT',
                'RECORD_LOVER', 'SENSE_SEEKER'),
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

CALL seed_users(100000);
ANALYZE TABLE users;

-- -------------------------------------------------------------
-- 2. user_mission_log 재적재 (100만 건 = 유저당 평균 10건, 2~4분 소요)
--   user_id가 1~100,000 균등이므로 100만 건 ÷ 10만 명 = 팬아웃 10.
--   VIP 회원당 주문 10건 상황과 동일한 분포.
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
    DECLARE v_cd DATE;
    DECLARE v_ca DATETIME(6);

    SET autocommit = 0;

    WHILE i < p_total DO
        SET v_cd = DATE_ADD('2026-01-01', INTERVAL FLOOR(RAND() * 365) DAY);
        SET v_ca = TIMESTAMP(v_cd)
                       + INTERVAL FLOOR(RAND() * 86400) SECOND
                       + INTERVAL IF(RAND() < 0.05, 1 + FLOOR(RAND() * 3), 0) DAY;

        INSERT INTO user_mission_log
            (idempotency_key, user_id, solar_term_id, completed_date, created_at, updated_at)
        VALUES (
            CONCAT('seed-', i),
            1 + FLOOR(RAND() * 100000),
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

CALL seed_user_mission_log(1000000);
ANALYZE TABLE user_mission_log;

-- 분포 검증: 유저당 로그 수 평균 ≈ 10 확인
SELECT COUNT(*) / COUNT(DISTINCT user_id) AS avg_logs_per_user,
       COUNT(*) AS total_rows
FROM user_mission_log;

SELECT user_type, COUNT(*) AS cnt FROM users GROUP BY user_type;


-- =============================================================
-- 실험 대상 쿼리 (서비스 시나리오: 같은 유저 타입 미션 랭킹 TOP 20)
--
--   member/orders 예시와 1:1 대응:
--     u.user_type = 'NATURE_OUTDOOR'  ≒ m.grade = 'VIP'
--     COUNT(l.id)                     ≒ COUNT(o.order_id)
--     MAX(l.created_at)               ≒ SUM(o.total_price)
--                                       (접근 경로엔 불필요, SELECT에만 필요
--                                        → 커버링 전엔 행별 테이블 룩업 강제)
--
--   기대 팬아웃: NATURE_OUTDOOR 2만 명 × 유저당 10건
--     → Index lookup on l (user_id=u.id) rows≈10 loops≈20000
--     = 랜덤 액세스 약 20만 번 (예시의 60000 × 10 = 60만과 동일 구조)
--
--   ORDER BY가 집계 결과라 "드리븐 먼저 정렬 → 조기종료" 지름길 불가
--   → 조인을 끝까지 해야 하므로 등호 필터 쪽(users)이 드라이빙이 된다
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