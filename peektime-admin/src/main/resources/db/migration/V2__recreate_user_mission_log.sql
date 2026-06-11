-- user_mission_log 테이블 재생성
-- userUuid(VARCHAR) -> userId(BIGINT) 변경

DROP TABLE IF EXISTS user_mission_log;

CREATE TABLE user_mission_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    solar_term_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_user_id ON user_mission_log(user_id);
CREATE INDEX idx_solar_term_id ON user_mission_log(solar_term_id);