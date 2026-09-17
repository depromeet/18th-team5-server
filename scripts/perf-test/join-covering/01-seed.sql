-- =============================================================
-- 조인 + 커버링 인덱스 실험용 users 테이블 적재 (MySQL 8 전용)
--
-- 전제: user-mission-log/01-seed.sql 로 perf_test.user_mission_log
--       500만 건이 이미 적재돼 있어야 한다 (재적재 불필요, 그대로 재활용).
--
-- 사용법:
--   mysql -uroot -p < 01-seed.sql
--   또는 docker: docker exec -i perf-mysql mysql -uroot -proot < 01-seed.sql
--
--   10만 건 기준 수십 초 소요.
-- =============================================================

USE perf_test;

-- 조인 드라이빙 테이블. id가 user_mission_log.user_id(1~100,000)와
-- 정확히 맞물리도록 AUTO_INCREMENT 순차 채번으로 10만 건 적재한다.
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname VARCHAR(50) NOT NULL,
    user_type VARCHAR(30) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- 데이터 분포 설계 (수치 역산 근거):
--   user_type : 5종 균등 → 타입당 2만 명 (선택도 20%).
--               WHERE 등호 조건이 "풀스캔 10만 → 인덱스 2만"을 보여주는 축
--   nickname  : 시퀀스 기반 → 커버링 실험에서 SELECT 대상이 되는 비인덱스 컬럼
--
--   조인 팬아웃 : 로그가 유저당 평균 50건(500만/10만)이므로
--                completed_date 범위를 73일(연중 20%)로 잡으면
--                드리븐 lookup당 실제 rows ≈ 50 × 20% ≈ 10건
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

SELECT user_type, COUNT(*) AS cnt
FROM users
GROUP BY user_type;


-- =============================================================
-- 실험 대상 쿼리 (서비스 시나리오: 같은 유저 타입 유저들의 최근 미션 완료 피드)
--
--   드라이빙 users     : WHERE 등호(user_type) + SELECT nickname
--   드리븐 mission_log : 조인키(user_id) + 범위(completed_date)
--                        + SELECT solar_term_id + ORDER BY created_at
--   → NLJ + Using temporary + Using filesort + 커버링 실험 요소 전부 포함
-- =============================================================
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;


-- =============================================================
-- [옵션] 정리
-- =============================================================
-- DROP TABLE IF EXISTS users;