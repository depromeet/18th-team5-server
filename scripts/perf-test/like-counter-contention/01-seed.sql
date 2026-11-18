-- =============================================================
-- 좋아요 카운터 경합 실험용 seed — 핫 피드 항목 1건 (MySQL 8 전용)
-- 대상: peaktime_db
--
-- 전제: 앱을 loadtest(=prod 베이스, ddl-auto: update) 프로필로 한 번 기동해서
--       like_count 컬럼과 feed_like 테이블이 생성된 상태여야 한다. 테이블 DDL의
--       주인은 이 스크립트가 아니라 엔티티이므로 여기서 CREATE 하지 않는다.
--
-- 적재 대상: 좋아요가 몰릴 "인기 피드 1건". 완료(user_mission_completion) 한 행만 있으면
--   된다 — feed_like 행들은 부하 테스트가 직접 만든다(완료 FK와 달리 feed_like는 FK가 없다).
--
-- 사용법:
--   mysql -uroot -p peaktime_db < 01-seed.sql
-- =============================================================

USE peaktime_db;

-- Workbench 안전 업데이트 모드 해제 — completion_id는 유니크 제약(user_id, completion_id)의
-- 선두 컬럼이 아니라 키로 안 쳐줘서, 이게 없으면 아래 DELETE에서 1175가 난다 (세션 한정)
SET SQL_SAFE_UPDATES = 0;

-- 핫 completion의 고정 id (앱이 채번하는 대역과 겹치지 않게 큰 값 사용)
SET @HOT := 8000001;

-- 재실행 대비 초기화 (FK off: 더미 user/mission/solar_term을 참조하므로)
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM feed_like WHERE completion_id = @HOT;
DELETE FROM user_mission_completion WHERE id = @HOT;

-- 핫 피드 항목: object_key 채워 피드 노출 대상이 되게 하고 like_count는 0에서 시작.
-- user_id/mission_id/solar_term_id는 실존하지 않는 더미(FK off라 통과) — 좋아요 경로는
-- 이 행의 like_count만 UPDATE할 뿐 부모 테이블을 건드리지 않는다.
INSERT INTO user_mission_completion
    (id, user_id, mission_id, solar_term_id, mission_type,
     object_key, completed_date, like_count, created_at, updated_at)
VALUES
    (@HOT, @HOT, @HOT, @HOT, 'DAILY',
     CONCAT('photo/loadtest/', @HOT, '.jpg'), CURDATE(), 0, NOW(6), NOW(6));

SET FOREIGN_KEY_CHECKS = 1;

-- 확인
SELECT id, object_key, like_count FROM user_mission_completion WHERE id = @HOT;

-- -------------------------------------------------------------
-- 재실행용 리셋 (부하 한 번 돌린 뒤 다시 깨끗하게 시작할 때만)
-- -------------------------------------------------------------
-- SET SQL_SAFE_UPDATES = 0;
-- SET @HOT := 8000001;
-- DELETE FROM feed_like WHERE completion_id = @HOT;
-- UPDATE user_mission_completion SET like_count = 0 WHERE id = @HOT;