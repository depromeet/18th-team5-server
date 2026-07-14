-- Mission ID 채번을 admin ID 할당 방식 → auto increment로 전환
--
-- 배경: 기존에는 "Admin ID = API ID" 전략이라 Mission PK를 직접 할당했고,
--       LLM 생성 미션은 충돌 방지를 위해 별도 대역(1,000,000~)을 분산 락으로 채번했다.
--       admin 원본 ID를 admin_mission_id 매핑 컬럼으로 분리하면서
--       PK는 auto increment로, 락은 unique 제약으로 대체한다.
--
-- 실행 시점: 이 변경이 포함된 API 배포 **전에** 운영 MySQL에서 순서대로 실행
--            (API 모듈은 Flyway 없이 ddl-auto: update라 백필과 PK 변경을 자동 반영하지 못함)
-- 전제: LLM 미션 생성 기능(PICK-247 브랜치)이 아직 미배포 → 운영의 기존 미션은 전부 admin 동기화 미션

-- 0) LLM 생성 미션 표시 컬럼 (이 브랜치에서 추가된 컬럼. 기존 미션은 모두 false)
ALTER TABLE mission ADD COLUMN llm_generated TINYINT(1) NOT NULL DEFAULT 0;

-- 1) admin 원본 ID 매핑 컬럼 추가
ALTER TABLE mission ADD COLUMN admin_mission_id BIGINT NULL;

-- 2) 기존 미션 백필 — 전부 admin 동기화 미션이므로 전체 대상
UPDATE mission SET admin_mission_id = id;

-- 3) 동기화 upsert 매칭 키 unique 보장 (NULL은 중복 허용되므로 이후 LLM 미션과 공존 가능)
ALTER TABLE mission ADD CONSTRAINT uk_mission_admin_mission_id UNIQUE (admin_mission_id);

-- 4) PK를 auto increment로 전환 (counter는 MySQL이 max(id)+1로 자동 설정)
--    mission.id를 참조하는 FK들 때문에 MODIFY가 막히므로(Error 1833) FK 검사를 세션 안에서 잠깐 끈다.
--    타입은 BIGINT 그대로 유지되므로 FK 정합성에 영향 없음. 반드시 같은 세션에서 연속 실행.
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE mission MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;
SET FOREIGN_KEY_CHECKS = 1;

-- 5) 선택 미션 하루 1개 정책의 DB 레벨 보장 (분산 락의 최종 방어선)
--    실행 전 중복 데이터 확인:
--    SELECT user_id, selected_date, COUNT(*) FROM user_selected_mission
--    GROUP BY user_id, selected_date HAVING COUNT(*) > 1;
ALTER TABLE user_selected_mission
    ADD CONSTRAINT uk_user_selected_mission_user_date UNIQUE (user_id, selected_date);

-- 실행 후 검증
-- SELECT COUNT(*) FROM mission WHERE admin_mission_id IS NULL;  -- 0이어야 함
-- SHOW CREATE TABLE mission;                                    -- id에 AUTO_INCREMENT 확인