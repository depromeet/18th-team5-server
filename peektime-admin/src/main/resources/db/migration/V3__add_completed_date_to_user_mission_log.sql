-- user_mission_log에 미션 완료 날짜 컬럼 추가
-- 일 단위 집계·보상은 로그 도착 시각(created_at)이 아니라 완료 날짜 기준이어야
-- 자정 직전 완료 건이 재시도로 늦게 도착해도 실적 날짜가 어긋나지 않는다

ALTER TABLE user_mission_log ADD COLUMN completed_date DATE NULL;

-- 기존 row 백필: 완료 날짜를 알 수 없으므로 도착일로 근사
UPDATE user_mission_log SET completed_date = DATE(created_at) WHERE completed_date IS NULL;

ALTER TABLE user_mission_log MODIFY COLUMN completed_date DATE NOT NULL;

-- 일 단위 집계 쿼리용
CREATE INDEX idx_completed_date ON user_mission_log(completed_date);