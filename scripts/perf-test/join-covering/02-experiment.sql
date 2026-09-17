-- =============================================================
-- 조인 + 커버링 인덱스 단계별 실험 스크립트
--
-- 대상 쿼리 (같은 유저 타입 유저들의 최근 미션 완료 피드 20건):
--
--   SELECT u.nickname, l.solar_term_id, l.completed_date
--   FROM users u
--   JOIN user_mission_log l ON l.user_id = u.id
--   WHERE u.user_type = 'NATURE_OUTDOOR'
--     AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
--   ORDER BY l.created_at DESC
--   LIMIT 20;
--
-- 각 단계를 순서대로 실행하며 EXPLAIN ANALYZE 결과를 비교한다.
-- 볼 것:
--   - "Nested loop inner join" 유무와 각 테이블의 접근 방식
--   - 드리븐 lookup의 rows=?? loops=?? (팬아웃 × 반복 횟수 = 랜덤 I/O 규모)
--   - "Index lookup" vs "Covering index lookup" (테이블 룩업 유무)
--   - EXPLAIN(비ANALYZE)의 Extra: Using temporary; Using filesort
--
-- ※ 랜덤 I/O 절감 효과는 버퍼 풀이 식은(콜드) 첫 실행에서 극적이다.
--   두 번째 실행부터는 페이지가 메모리에 올라와 차이가 줄어드니
--   단계별 "첫 실행" actual time을 비교하거나, 반복 실행 시에는
--   rows × loops(터치한 행 수)와 접근 방식 변화를 근거로 해석한다.
-- =============================================================

USE perf_test;


-- -------------------------------------------------------------
-- [0단계] users에 인덱스 없음 (log는 기본 idx_user_id만)
-- 예상: WHERE u.user_type을 태울 인덱스가 없어 users 풀스캔(10만 행)
--       → 이게 "WHERE 절 풀스캔" 문제 상황.
--       드리븐은 idx_user_id로 NLJ. (비용 추정에 따라 옵티마이저가
--       해시 조인 + log 풀스캔을 고를 수도 있음 — 그 자체가 관찰 포인트)
-- filesort 확인: 일반 EXPLAIN의 Extra에 Using temporary; Using filesort
-- -------------------------------------------------------------
EXPLAIN
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;

EXPLAIN ANALYZE
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [1단계] users(user_type) 단일 인덱스 → WHERE 풀스캔 제거
-- 예상: 드라이빙이 ref 접근(2만 행)으로 바뀜.
--       단, nickname이 인덱스에 없어 유저당 1회 테이블 룩업 (2만 회)
--       드리븐은 idx_user_id lookup → rows≈50 loops≈20000,
--       completed_date 필터는 테이블 룩업 후 행별 체크(filtered≈20%)
--       → 드리븐 쪽 랜덤 I/O 약 100만 회 (50 × 2만)
-- -------------------------------------------------------------
ALTER TABLE users ADD INDEX idx_user_type (user_type);
ANALYZE TABLE users;

EXPLAIN ANALYZE
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u FORCE INDEX (idx_user_type)
JOIN user_mission_log l FORCE INDEX (idx_user_id) ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [2단계] 드리븐 복합 인덱스: (user_id, completed_date)
-- 예상: 조인키 등호 + 날짜 범위가 인덱스에서 한 번에 좁혀져
--       드리븐 lookup의 rows가 50 → 약 10으로 감소 (팬아웃 축소).
--       단, SELECT의 solar_term_id와 ORDER BY의 created_at이 인덱스에
--       없어 좁힌 10행에 대해선 여전히 테이블 룩업 (10 × 2만 = 20만 회)
--       → 랜덤 I/O가 100만 → 20만으로 줄지만 0은 아님
-- -------------------------------------------------------------
ALTER TABLE user_mission_log ADD INDEX idx_uid_cd (user_id, completed_date);
ANALYZE TABLE user_mission_log;

EXPLAIN ANALYZE
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u FORCE INDEX (idx_user_type)
JOIN user_mission_log l FORCE INDEX (idx_uid_cd) ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [3단계] 양쪽 모두 커버링 → 테이블 랜덤 I/O 0회
--   드라이빙: (user_type, nickname)
--             — WHERE 등호 + SELECT nickname + 조인키 id(PK 암묵 포함) 전부 커버
--   드리븐  : (user_id, completed_date, solar_term_id, created_at)
--             — 조인키 + 범위 + SELECT + 정렬 컬럼 전부 커버
--             (앞 2개 컬럼 순서만 접근 경로에 쓰이고, 뒤 2개는 커버 용도라 순서 무관)
-- 예상: 두 테이블 모두 "Covering index lookup / Covering index range scan"
--       → 예시 쿼리의 최종 플랜과 같은 모양. 2만 + 20만 번의 테이블
--       룩업이 전부 사라지고 인덱스 순차 읽기만 남는다.
-- ※ Using temporary; Using filesort는 여전히 남는다 — 2만 명의 결과가
--   섞여 정렬되므로 어떤 인덱스 순서로도 ORDER BY를 못 푼다.
--   이 실험의 개선 축은 정렬 제거가 아니라 정렬 "입력을 만드는 비용"(랜덤 I/O)
-- -------------------------------------------------------------
ALTER TABLE users ADD INDEX idx_type_nick (user_type, nickname);
ALTER TABLE user_mission_log ADD INDEX idx_uid_cd_st_ca
    (user_id, completed_date, solar_term_id, created_at);
ANALYZE TABLE users;
ANALYZE TABLE user_mission_log;

EXPLAIN ANALYZE
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u FORCE INDEX (idx_type_nick)
JOIN user_mission_log l FORCE INDEX (idx_uid_cd_st_ca) ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;

-- 3-b. SELECT * 로 바꾸면 커버링이 깨지는 것 확인
--      (Covering 접두어가 사라지고 테이블 룩업 부활)
EXPLAIN ANALYZE
SELECT *
FROM users u FORCE INDEX (idx_type_nick)
JOIN user_mission_log l FORCE INDEX (idx_uid_cd_st_ca) ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;


-- -------------------------------------------------------------
-- [4단계] 옵티마이저 자유 선택 + NLJ vs 해시 조인 비교
-- -------------------------------------------------------------
-- 4-a. 힌트 없이: 인덱스가 다 있는 상태에서 3단계 플랜을 스스로 고르는지
EXPLAIN ANALYZE
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u
JOIN user_mission_log l ON l.user_id = u.id
WHERE u.user_type = 'NATURE_OUTDOOR'
  AND l.completed_date BETWEEN '2026-08-01' AND '2026-10-12'
ORDER BY l.created_at DESC
LIMIT 20;

-- 4-b. 드리븐 인덱스를 전부 무시 → 조인키 인덱스가 없으면 MySQL 8은
--      해시 조인(log 풀스캔 500만 행 + 해시 빌드)으로 전환.
--      "인덱스 lookup 20만 번 vs 풀스캔 500만 행" 비용 비교 관찰
EXPLAIN ANALYZE
SELECT u.nickname, l.solar_term_id, l.completed_date
FROM users u
JOIN user_mission_log l IGNORE INDEX (idx_user_id, idx_uid_cd, idx_uid_cd_st_ca)
    ON l.user_id = u.id
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
-- ALTER TABLE user_mission_log DROP INDEX idx_uid_cd_st_ca;
-- ANALYZE TABLE users;
-- ANALYZE TABLE user_mission_log;