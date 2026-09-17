-- =============================================================
-- 조인 + 커버링 인덱스 단계별 실험 스크립트
--
-- 대상 쿼리 (이번 절기 구간 같은 유저 타입 미션 랭킹 TOP 20):
--
--   SELECT u.id, u.nickname,
--          COUNT(l.id) AS mission_count,
--          MAX(l.created_at) AS last_completed_at
--   FROM users u
--   JOIN user_mission_log l ON l.user_id = u.id
--   WHERE u.user_type = 'NATURE_OUTDOOR'
--     AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
--   GROUP BY u.id, u.nickname
--   ORDER BY mission_count DESC
--   LIMIT 20;
--
-- 쿼리 모양이 중요하다: ORDER BY가 집계 결과라 "드리븐을 먼저 정렬 후
-- 조기종료" 지름길이 불가능 → 힌트 없이도 users → log 방향 NLJ.
-- GROUP BY는 드라이빙 행 단위로 스트리밍(Group aggregate)되므로
-- temporary 없이 집계 후 Sort(filesort)만 남는다 — 예시 플랜의
-- "Stream results → Group aggregate → Sort" 구조와 동일.
--
-- 각 단계에서 볼 것:
--   - Nested loop inner join 아래 두 테이블의 접근 방식
--   - 드리븐 lookup의 rows ≈ 10, loops ≈ 20000 (랜덤 액세스 규모 = rows × loops)
--   - "Index lookup" → "Covering index lookup" 전환 (테이블 룩업 소멸)
--
-- ※ 측정 팁: 시드 직후엔 500만 건이 버퍼 풀/OS 캐시에 전부 올라와 있어
--   랜덤 I/O가 메모리 읽기가 되고 시간 차이가 뭉개진다. 시간 대신
--   행 터치량으로 비교하거나(아래 Handler 통계), 콜드 측정은 MySQL 재시작 후 1회차.
--
--     FLUSH STATUS;
--     (대상 쿼리 실행)
--     SHOW STATUS LIKE 'Handler_read%';   -- Handler_read_rnd/next 등 행 접근 횟수
--     SHOW STATUS LIKE 'Innodb_buffer_pool_read_requests';  -- 페이지 접근 횟수
--
-- ※ 손으로 만든 임시 인덱스(test1 등)가 남아 있으면 플랜이 왜곡되니
--   실험 전 SHOW INDEX로 확인하고 제거할 것.
-- =============================================================

USE perf_test;

SHOW INDEX FROM users;
SHOW INDEX FROM user_mission_log;


-- -------------------------------------------------------------
-- [0단계] users에 인덱스 없음 (log는 기본 idx_user_id만)
-- 예상: WHERE u.user_type을 태울 인덱스가 없어 users 풀스캔(10만 행)
--       → "WHERE 절 풀스캔" 문제 상황. 스캔하며 user_type 행별 필터(20%)
-- -------------------------------------------------------------
EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [1단계] users(user_type) 단일 인덱스 → WHERE 풀스캔 제거
-- 예상: 드라이빙이 ref 접근(2만 행)으로 바뀜.
--       단, nickname이 인덱스에 없어 유저당 1회 테이블 룩업 (2만 회)
--       드리븐은 idx_user_id lookup → rows≈50 loops≈20000.
--       completed_date 필터·created_at 읽기가 테이블에서 일어나므로
--       드리븐 쪽 랜덤 액세스 약 100만 회 (50 × 2만)
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
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [2단계] 드리븐 복합 인덱스: (user_id, completed_date)
-- 예상: 조인키 등호 + 날짜 범위가 인덱스에서 한 번에 좁혀져
--       드리븐 lookup의 rows가 50 → 약 10으로 감소
--       (예시 플랜의 "rows=9.98 loops=60000"과 같은 모양).
--       COUNT(l.id)와 날짜 조건은 인덱스로 해결되지만 MAX(l.created_at)
--       때문에 좁힌 10행은 여전히 테이블 룩업
--       → 랜덤 액세스 100만 → 20만 회 (10 × 2만). 줄었지만 0은 아님
-- -------------------------------------------------------------
ALTER TABLE user_mission_log ADD INDEX idx_uid_cd (user_id, completed_date);
ANALYZE TABLE user_mission_log;

EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [3단계] 양쪽 모두 커버링 → 테이블 랜덤 액세스 0회
--   드라이빙: (user_type, nickname)
--             — WHERE 등호 + SELECT nickname + 조인키 id(PK 암묵 포함) 커버
--               (예시의 idx_member_grade_name과 같은 역할)
--   드리븐  : (user_id, completed_date, created_at)
--             — 조인키 + 범위 + MAX(created_at) + COUNT(l.id)(PK 포함) 커버
--               (예시의 idx_order_price와 같은 역할)
-- 예상: 두 테이블 모두 "Covering index lookup" — 예시 최종 플랜과 동일 구조.
--       2만 + 20만 번의 테이블 룩업이 전부 사라지고 인덱스 읽기만 남는다.
--       유저당 10행이 인덱스 리프에서 연속이라 루프당 1~2페이지로 압축
-- -------------------------------------------------------------
ALTER TABLE users ADD INDEX idx_type_nick (user_type, nickname);
ALTER TABLE user_mission_log ADD INDEX idx_uid_cd_ca (user_id, completed_date, created_at);
ANALYZE TABLE users;
ANALYZE TABLE user_mission_log;

EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;

-- 3-b. 드리븐 SELECT에 커버 안 된 컬럼(solar_term_id)을 추가하면
--      커버링이 깨지는 것 확인 → 20만 번 테이블 룩업 부활
EXPLAIN ANALYZE
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at,
       COUNT(DISTINCT l.solar_term_id) AS term_variety
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [비교] 왜 집계 없는 피드형 쿼리로는 이 실험이 안 됐는가
-- ORDER BY가 드리븐 컬럼 단독(l.created_at)이면 옵티마이저가
-- "l을 먼저 정렬 → 위에서부터 u를 PK로 확인 → LIMIT 조기종료" 플랜을
-- 선택한다. 정렬이 조인 아래로 내려가 temporary가 사라지고,
-- 드리븐이 u(PK 등호, rows=1)로 뒤집혀 팬아웃 10짜리 NLJ가 소멸.
-- → 같은 테이블·같은 인덱스인데 쿼리 모양이 조인 방향을 바꾸는 사례
-- -------------------------------------------------------------
EXPLAIN ANALYZE
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [초기화] 실험 인덱스 제거 → 0단계 상태로 복원
-- -------------------------------------------------------------
-- ALTER TABLE users DROP INDEX idx_user_type;
-- ALTER TABLE users DROP INDEX idx_type_nick;
-- ALTER TABLE user_mission_log DROP INDEX idx_uid_cd;
-- ALTER TABLE user_mission_log DROP INDEX idx_uid_cd_ca;
-- ANALYZE TABLE users;
-- ANALYZE TABLE user_mission_log;
