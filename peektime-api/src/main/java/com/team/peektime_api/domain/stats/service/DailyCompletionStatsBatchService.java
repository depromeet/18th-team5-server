package com.team.peektime_api.domain.stats.service;

import com.team.peektime_api.domain.stats.entity.DailyCompletionStats;
import com.team.peektime_api.domain.stats.repository.DailyCompletionAggregationRepository;
import com.team.peektime_api.domain.stats.repository.DailyCompletionStatsRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyCompletionStatsBatchService {

    private final DailyCompletionStatsRepository dailyCompletionStatsRepository;
    private final DailyCompletionAggregationRepository dailyCompletionAggregationRepository;

    // "어제 하루"가 아니라 "마지막 확정일 다음날 ~ 어제"를 확정한다 — 배치가 며칠 빠져도
    // 다음 실행이 공백을 메운다(캐치업). 증분이 아닌 절대값 재집계라 몇 번을 다시 돌려도
    // 같은 결과(멱등). DELETE와 INSERT가 한 트랜잭션이라 조회가 중간 상태(날짜 공백)를 보지 않는다
    @Transactional
    public void confirmStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 통계가 기록된 마지막 날짜 = 확정 경계. 여기까지는 동결, 건드리지 않는다
        LocalDate lastConfirmed = dailyCompletionStatsRepository.findMaxStatDate().orElse(null);

        // 확정 시작일 결정
        LocalDate from;
        if (lastConfirmed != null) {
            // 평소: 마지막 확정일 다음날부터
            from = lastConfirmed.plusDays(1);
        } else {
            // 최초 배치(통계 테이블이 빔): 원본의 가장 오래된 완료일부터 전체 백필
            from = dailyCompletionAggregationRepository.findMinCompletedDate().orElse(null);
        }

        // from == null: 원본에 완료 기록이 한 건도 없음 / from > 어제: 어제까지 이미 다 확정됨
        if (from == null || from.isAfter(yesterday)) {
            log.info("일별 통계 확정 배치 — 확정할 날짜 없음 (확정 경계: {})", lastConfirmed);
            return;
        }

        List<DailyCompletionStats> stats = dailyCompletionAggregationRepository
                .aggregateDailyStats(from, yesterday).stream()
                .map(p -> DailyCompletionStats.create(p.getStatDate(), p.getCompletionCount(), p.getPhotoCount()))
                .toList();

        dailyCompletionStatsRepository.deleteByStatDateBetween(from, yesterday);
        dailyCompletionStatsRepository.saveAll(stats);

        log.info("일별 통계 확정 배치 — {} ~ {} 확정, {}건 (기존 확정 경계: {})",
                from, yesterday, stats.size(), lastConfirmed);
    }
}