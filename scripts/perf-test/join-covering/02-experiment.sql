-- =============================================================
-- 조인 + 커버링 인덱스 단계별 실험 스크립트 (편중 분포 버전)
--
-- 전제: 01-seed.sql 실행 완료
--   users 250만 (NATURE_OUTDOOR 2% ≈ 5만 명)
--   user_mission_log 약 420만 (NATURE만 평균 10건, 전체 평균 ≈ 2건)
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
-- ★ 이 실험의 주제: 편중 데이터에서 추정과 실측의 괴리
--   rec_per_key(인덱스 통계)는 "전체 평균 2건"만 안다.
--   NATURE_OUTDOOR 세그먼트만 10건이라는 편중은 통계에 없다.
--   → EXPLAIN(추정)은 드리븐 rows≈2, EXPLAIN ANALYZE(실측)는 rows≈10.
--   옵티마이저는 NLJ를 "5만 × 2 = 10만 룩업"으로 싸게 보고 골랐지만
--   실제로는 5만 × 10 = 50만 랜덤 I/O — 추정의 5배가 터진다.
--   커버링 인덱스는 이 "추정 밖의 랜덤 I/O"를 구조적으로 제거한다.
--
-- 각 단계에서 볼 것:
--   - EXPLAIN의 rows(추정 2.x) vs EXPLAIN ANALYZE의 rows(실측 ≈10) 괴리
--   - 드리븐 lookup의 rows≈10 loops≈50000 (랜덤 액세스 = rows × loops)
--   - "Index lookup" → "Covering index lookup" 전환 (테이블 룩업 소멸)
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
-- 예상: WHERE u.user_type을 태울 인덱스가 없어 users 풀스캔(250만 행)
--       → "WHERE 절 풀스캔" 문제 상황. 스캔하며 user_type 행별 필터(2%)
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
-- 예상 (member/orders 기준 플랜과 동일 모양):
--   -> Index lookup on u using idx_user_type (rows≈50000)  ≒ idx_member_grade
--   -> Index lookup on l using idx_user_id
--      추정 rows≈2 (전체 평균!) / 실측 rows≈10 loops≈50000  ≒ 예시의 2.63 vs 9.98
--
--   ★ 먼저 EXPLAIN(추정만)을 보고, EXPLAIN ANALYZE(실측)와 비교할 것.
--   드라이빙: nickname이 인덱스에 없어 유저당 1회 테이블 룩업 (5만 회)
--   드리븐  : MAX(l.created_at) 때문에 실측 10행 전부 테이블 룩업
--             → 옵티마이저 예상(10만)의 5배인 랜덤 액세스 약 50만 회
-- -------------------------------------------------------------
ALTER TABLE users ADD INDEX idx_user_type (user_type);
ANALYZE TABLE users;

-- 1-a. 추정치 확인: 드리븐 rows가 2로 찍히는 것 관찰 (편중을 모르는 통계)
EXPLAIN
SELECT u.id, u.nickname,
       COUNT(l.id) AS mission_count,
       MAX(l.created_at) AS last_completed_at
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
GROUP BY u.id, u.nickname
ORDER BY mission_count DESC
LIMIT 20;

-- 1-b. 실측: 같은 노드가 rows≈10 loops≈20000으로 찍히는 것 관찰
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
--       추정 밖에서 터지던 50만 번 테이블 룩업이 0회로 — 핵심 전환.
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
--      깨지는 것 확인 → 드리븐 테이블 룩업 50만 회 부활
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
-- [교훈 노트]
--
-- 1. rec_per_key는 세그먼트를 모른다: 인덱스 통계는 "전체 평균 2건"
--    하나뿐이라, 특정 타입만 10건인 편중은 추정에 반영되지 않는다.
--    EXPLAIN rows=2 / 실측 rows=10 — 히스토그램(ANALYZE ... UPDATE
--    HISTOGRAM)도 조인 ref 추정에는 쓰이지 않아 이 괴리는 못 고친다.
--    통계가 틀려도 플랜 선택은 NLJ로 옳았지만, 비용(랜덤 I/O)은
--    예상의 5배 — 이런 상황에서 커버링은 "추정이 틀려도 안전한" 방어다.
--
-- 2. 팬아웃은 쿼리로 깎는 게 아니라 데이터 분포여야 한다: 이전 설계
--    (유저당 50건 + 날짜 범위로 10건 축소)는 옵티마이저가 드리븐을
--    rec_per_key=50으로 과대평가 → users 드라이빙이 비싸 보여서
--    조인을 뒤집었다(l 풀스캔 + u PK lookup rows=1).
--
-- 3. ORDER BY가 드리븐 컬럼 단독이면 정렬이 조인 아래로 내려가
--    조기종료가 가능해져 역시 조인이 뒤집힌다. 집계 결과 정렬
--    (mission_count)이라 지름길이 봉쇄된 것도 이 실험의 필수 조건.
--
-- 4. 랜덤 I/O는 행 수(논리)가 아니라 페이지 산포(물리)가 만든다:
--    로그를 유저 순회로 적재했을 땐 한 유저의 10건이 같은 페이지에
--    연속이라 룩업 10번 ≈ 페이지 1개 읽기 — 테이블을 2.5배 키워도
--    룩업 단가(0.008ms)가 그대로였다. 한 건마다 무작위 유저 배정으로
--    바꾸자 10건이 서로 다른 페이지로 흩어져 진짜 랜덤 액세스가 됐다.
-- -------------------------------------------------------------


-- -------------------------------------------------------------
-- [초기화] 실험 인덱스 제거 → 0단계 상태로 복원
-- -------------------------------------------------------------
-- ALTER TABLE users DROP INDEX idx_user_type;
-- ALTER TABLE users DROP INDEX idx_type_nick;
-- ALTER TABLE user_mission_log DROP INDEX idx_uid_ca;
-- ANALYZE TABLE users;
-- ANALYZE TABLE user_mission_log;