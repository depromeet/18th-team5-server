-- =============================================================
-- 조인 + 커버링 인덱스 단계별 실험 스크립트
--
-- 전제: 01-seed.sql 실행 완료 (users 10만 + user_mission_log 100만, 유저당 10건)
--
-- 대상 쿼리 (같은 유저 타입 미션 랭킹 TOP 20):
--
--   SELECT u.id, u.nickname,
--          COUNT(l.id) AS mission_count,
--          MAX(l.created_at) AS last_completed_at
--   FROM users u
--   JOIN user_mission_log l ON l.user_id = u.id
--   WHERE u.user_type = 'NATURE_OUTDOOR'
--   GROUP BY u.id, u.nickname
--   ORDER BY mission_count DESC
--   LIMIT 20;
--
-- 각 단계에서 볼 것:
--   - Nested loop inner join: users(드라이빙) → log(드리븐) 방향인지
--   - 드리븐 lookup의 rows ≈ 10, loops ≈ 20000 (랜덤 액세스 = rows × loops)
--   - "Index lookup" → "Covering index lookup" 전환 (테이블 룩업 소멸)
--
-- ※ 조인이 뒤집히면(l 드라이빙 + u PK lookup rows=1) 손으로 만든
--   인덱스가 남아 있는지부터 확인할 것. l 쪽에 커버링 가능한 인덱스가
--   있으면 "l 인덱스 풀스캔 → u PK 확인" 경로가 싸져서 방향이 뒤집힌다.
--
-- ※ 측정 팁: 버퍼 풀이 데운 상태에선 랜덤 I/O 차이가 시간으로 잘 안 보인다.
--   시간 대신 행 터치량으로 비교:
--     FLUSH STATUS;  (대상 쿼리 실행)  SHOW STATUS LIKE 'Handler_read%';
-- =============================================================

USE perf_test;

-- 시작 전 인덱스 상태 확인 (기대: users는 PRIMARY만, log는 PRIMARY + UNIQUE(idempotency_key) + idx_user_id)
SHOW INDEX FROM users;
SHOW INDEX FROM user_mission_log;


-- -------------------------------------------------------------
-- [0단계] users에 인덱스 없음 (log는 idx_user_id만)
-- 예상: WHERE u.user_type을 태울 인덱스가 없어 users 풀스캔(10만 행)
--       → "WHERE 절 풀스캔" 문제 상황. 스캔하며 user_type 행별 필터(20%)
--       드리븐은 idx_user_id lookup rows≈10
-- -------------------------------------------------------------
EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [1단계] users(user_type) 단일 인덱스 → WHERE 풀스캔 제거
-- 예상 (member/orders "아무것도 안 했을 때" 플랜과 동일 모양):
--   -> Index lookup on u using idx_user_type (rows≈20000)   ≒ idx_member_grade
--   -> Index lookup on l using idx_user_id (rows≈10 loops≈20000)
--                                                            ≒ idx_orders_member_id
--   드라이빙: nickname이 인덱스에 없어 유저당 1회 테이블 룩업 (2만 회)
--   드리븐  : MAX(l.created_at) 때문에 10행 전부 테이블 룩업
--             → 랜덤 액세스 약 20만 회 (10 × 2만) ← 이게 개선 대상
-- -------------------------------------------------------------
ALTER TABLE users ADD INDEX idx_user_type (user_type);
ANALYZE TABLE users;

EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [2단계] 드리븐 커버링: (user_id, created_at)
-- 예상: 조인키 + MAX(created_at) + COUNT(l.id)(PK 암묵 포함)가 전부
--       인덱스에서 해결 → "Covering index lookup on l" (≒ idx_order_price)
--       20만 번 테이블 룩업이 0회로 — 이 실험의 핵심 전환.
--       유저당 10행이 인덱스 리프에서 연속이라 루프당 1~2페이지로 압축
-- -------------------------------------------------------------
ALTER TABLE user_mission_log ADD INDEX idx_uid_ca (user_id, created_at);
ANALYZE TABLE user_mission_log;

EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [3단계] 드라이빙도 커버링: (user_type, nickname)
-- 예상: WHERE 등호 + SELECT nickname + 조인키 id(PK 암묵 포함) 커버
--       → "Covering index lookup on u" (≒ idx_member_grade_name)
--       양쪽 테이블 룩업 0회 — member/orders 최종 플랜과 동일 구조 완성
-- -------------------------------------------------------------
ALTER TABLE users ADD INDEX idx_type_nick (user_type, nickname);
ANALYZE TABLE users;

EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;

-- 3-b. 커버 안 된 컬럼(solar_term_id)을 SELECT에 추가하면 커버링이
--      깨지는 것 확인 → 드리븐 테이블 룩업 20만 회 부활
EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at,
       COUNT(DISTINCT l.solar_term_id) AS term_variety
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [교훈 노트] 이전 설계(500만 건 + 날짜 범위)가 실패한 이유
--
-- "유저당 50건을 completed_date 범위로 10건으로 깎는" 쿼리는
-- 옵티마이저가 users 드라이빙 비용을 rec_per_key(유저당 50건)로만
-- 추정한다 — 조인 값이 상수가 아니라 범위 축소를 비용에 반영 못 함.
-- → u 드라이빙 = "2만 × 50 = 100만 룩업"으로 과대평가되어
--   l 풀스캔 + u PK lookup(드리븐 rows=1) 플랜이 항상 선택됐다.
-- 팬아웃 실험은 쿼리로 깎는 게 아니라 데이터 분포 자체가 팬아웃이어야 한다.
--
-- 또한 ORDER BY가 드리븐 컬럼 단독(l.created_at)이면 정렬이 조인
-- 아래로 내려가 조기종료가 가능해져 역시 조인이 뒤집힌다.
-- → 집계 결과 정렬(mission_count)이라 지름길이 봉쇄된 것도 필수 조건.
-- -------------------------------------------------------------


-- -------------------------------------------------------------
-- [초기화] 실험 인덱스 제거 → 0단계 상태로 복원
-- -------------------------------------------------------------
-- ALTER TABLE users DROP INDEX idx_user_type;
-- ALTER TABLE users DROP INDEX idx_type_nick;
-- ALTER TABLE user_mission_log DROP INDEX idx_uid_ca;
-- ANALYZE TABLE users;
-- ANALYZE TABLE user_mission_log;