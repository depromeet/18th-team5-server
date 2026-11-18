package com.team.peektime_api.domain.stats.scheduler;

import com.team.peektime_api.domain.stats.service.DailyCompletionStatsBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCompletionStatsScheduler {

    private final DailyCompletionStatsBatchService dailyCompletionStatsBatchService;

    // 자정 직후를 피해 새벽 2시 — completed_date는 트랜잭션 안에서 LocalDate.now()로 찍히므로
    // 자정 정각 실행은 23:59에 시작해 00:00 이후 커밋되는 어제 날짜 행을 놓칠 수 있다
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "dailyCompletionStatsConfirm", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void confirmDailyStats() {
        try {
            dailyCompletionStatsBatchService.confirmStats();
        } catch (DataAccessException e) {
            // 멱등한 재집계라 동시 실행 레이스(중복 키, 데드락 희생)는 "다른 인스턴스가
            // 이미 확정했다"는 신호일 뿐 — 데이터는 정답이므로 기록만 남기고 넘어간다
            log.warn("일별 통계 확정 배치 실패 — 다음 실행이 캐치업으로 메운다", e);
        }
    }
}