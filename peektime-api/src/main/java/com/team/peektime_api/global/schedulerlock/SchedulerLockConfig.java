package com.team.peektime_api.global.schedulerlock;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 다중 인스턴스에서 @Scheduled 작업의 플릿 전체 1회 실행 보장 (실행권 락).
 *
 * 적용 기준 — "플릿에서 한 번"이어야 하는 작업에만 건다:
 *  O 통계 확정 배치, 탈퇴 유저 삭제, 알림 발송 (N중 실행 = 중복 작업/중복 푸시)
 *  X 피드 캐시 동기화 — 인스턴스별 로컬 메모리 캐시 유지라 모든 인스턴스가 각자 돌아야 한다
 *  X 아웃박스 폴러 — SKIP LOCKED + claim 리스로 이미 다중 인스턴스 안전, 병렬 claim이 의도
 *
 * 락은 임대(lease) 방식이라 lockAtMostFor 만료 엣지에서 드물게 중복 실행이 가능하다 —
 * 락은 중복 "확률"을 없애는 층이고, 정확성은 각 작업의 멱등성이 보장한다 (이중 구조).
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        // 임대 판정을 DB 서버 시간으로 — 인스턴스 간 시계 오차가 락 판정에 안 들어온다
                        .usingDbTime()
                        .build());
    }
}